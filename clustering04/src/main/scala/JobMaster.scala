package example

import akka.actor.typed.scaladsl.{ Behaviors, PoolRouter, Routers }
import akka.actor.typed.{ ActorRef, Behavior, SupervisorStrategy }

import scala.concurrent.duration._

object JobMaster {

  sealed trait Command

  case class StartJob(
      name: String,
      texts: List[String],
      director: ActorRef[Director.Command])
      extends Command
  case class JobStatus(
      name: String,
      textParts: Vector[List[String]] = Vector(),
      intermediateResult: Vector[Map[String, Int]] = Vector(),
      workGiven: Int = 0,
      workReceived: Int = 0,
      workers: Set[ActorRef[JobWorker.Command]] = Set())
      extends Command {

    def jobDone = workGiven == workReceived

  }
  case class Enlist(worker: ActorRef[JobWorker.Command])
      extends Command
  case class NextTask(worker: ActorRef[JobWorker.Command])
      extends Command
  case class TaskResult(wordsCount: Map[String, Int]) extends Command

  def apply() = Behaviors.setup[Command] { context =>

    val workers = context.spawn(
      Routers
        .pool(poolSize = 100) {
          Behaviors
            .supervise(JobWorker())
            .onFailure[Exception](SupervisorStrategy.restart)
        }
        .withBroadcastPredicate(_ => true),
      "workers-pool")

    Behaviors.receiveMessage {
      case StartJob(jobName, texts, director) =>
        val timerKey = s"timer-$jobName"
        workers ! JobWorker.Work(jobName, context.self)
        working(
          director,
          JobStatus(name = jobName, textParts = Vector(texts)))

      case _ => Behaviors.unhandled
    }
  }

  def working(
      director: ActorRef[Director.Command],
      jobStatus: JobStatus): Behavior[Command] =
    Behaviors.receive[Command] {
      case (_, Enlist(worker)) =>
        working(
          director,
          jobStatus.copy(workers = jobStatus.workers + worker)
        ) // odd I want to add it on front
      case (context, NextTask(worker)) =>
        if (jobStatus.textParts.isEmpty) {
          worker ! JobWorker.WorkLoadDepleted(jobStatus.name)
          Behaviors.same
        } else {
          worker ! JobWorker.Task(
            jobStatus.textParts.head,
            context.self)

          val newJobStatus = jobStatus
            .copy(workGiven = jobStatus.workGiven + 1)
            .copy(textParts = jobStatus.textParts.tail)

          working(director, newJobStatus)
        }

      case (_, TaskResult(wordsCountMap)) =>
        val newJobStatus =
          jobStatus
            .copy(workReceived = jobStatus.workReceived + 1)
            .copy(intermediateResult =
              jobStatus.intermediateResult :+ wordsCountMap)

        if (newJobStatus.textParts.isEmpty && newJobStatus.jobDone) {
          director ! Director.JobSuccess(
            newJobStatus.name,
            newJobStatus.intermediateResult.head)
          Behaviors.same
        } else {
          working(director, newJobStatus)
        }

      case _ => Behaviors.unhandled
    }

}
