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
      extends Command {

    def jobDone = workGiven == workReceived

  }
  case class Enlist(worker: ActorRef[JobWorker.Command])
      extends Command
  case class NextTask(worker: ActorRef[JobWorker.Command])
      extends Command
  case class TaskResult(wordsCount: Map[String, Int]) extends Command
  case object MergeResults extends Command
  case object Timeout extends Command

  def apply() = Behaviors.setup[Command] { context =>

    val workers = context.spawn(
      Routers
        .pool(poolSize = 100)(JobWorker())
        .withBroadcastPredicate(_ => true),
      "workers-pool")

    Behaviors.withTimers { timers =>
      Behaviors.receiveMessage {
        case StartJob(jobName, texts, director) =>
          val timerKey = s"timer-$jobName"
          workers ! JobWorker.Work(jobName, context.self)
          //after getting this message will cancel the job?
          timers.startSingleTimer(timerKey, Timeout, 60 seconds)
          working(
            director,
            timerKey,
            JobStatus(name = jobName, textParts = Vector(texts)))
      }
    }
  }

  def working(
      director: ActorRef[Director.Command],
      timerTimeoutKey: String,
      jobStatus: JobStatus): Behavior[Command] =
    Behaviors.setup { context =>
      Behaviors.receiveMessage[Command] {
        case Enlist(worker) =>
          working(
            director,
            timerTimeoutKey,
            jobStatus.copy(workers = jobStatus.workers + worker)
          ) // odd I want to add it on front
        case NextTask(worker) =>
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

            working(director, timerTimeoutKey, newJobStatus)
          }

        case TaskResult(wordsCountMap) =>
          val newJobStatus =
            jobStatus
              .copy(workReceived = jobStatus.workReceived + 1)
              .copy(intermediateResult =
                jobStatus.intermediateResult :+ wordsCountMap)

          if (newJobStatus.textParts.isEmpty && newJobStatus.jobDone) {
            context.self ! MergeResults
            finishing(newJobStatus, director)
          } else {
            working(director, timerTimeoutKey, newJobStatus)
          }

      }
    }

  def finishing(
      jobStatus: JobStatus,
      director: ActorRef[Director.Command]) =
    Behaviors.receive[Command] {
      case (context, MergeResults) =>
        //not merging by now
        director ! Director.WordCount(
          jobStatus.name,
          jobStatus.intermediateResult.head)
        Behaviors.stopped
      case (_, _) => Behaviors.unhandled
    }

}
