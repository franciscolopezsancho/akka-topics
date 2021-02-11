package example

import akka.actor.typed.scaladsl.{ Behaviors, PoolRouter, Routers }
import akka.actor.typed.{ ActorRef, Behavior }

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
      extends Command

  case class Enlist(worker: ActorRef[JobWorker.Command])
      extends Command
  case class NextTask(worker: ActorRef[JobWorker.Command])
      extends Command

  case object Timeout extends Command

  def apply() = Behaviors.setup[Command] { context =>

    val workers = context.spawn(
      Routers
        .pool(poolSize = 100)(JobWorker())
        .withBroadcastPredicate(_ => true),
      "workers-pool")

    Behaviors.receiveMessage {
      case StartJob(jobName, texts, director) =>
        workers ! JobWorker.Work(jobName, context.self)
        working(director, JobStatus(name = jobName))
      case _ => Behaviors.unhandled
    }
  }

  def working(
      director: ActorRef[Director.Command],
      jobStatus: JobStatus): Behavior[Command] =
    Behaviors.receive {
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

          val newJobStatus = jobStatus.copy(
            workGiven = jobStatus.workGiven + 1,
            textParts = jobStatus.textParts.tail)
          working(director, newJobStatus)
        }
      case (_, _) => Behaviors.unhandled
    }

}
