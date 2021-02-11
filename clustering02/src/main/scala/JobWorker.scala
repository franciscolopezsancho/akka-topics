package example

import akka.actor.typed.scaladsl.{ Behaviors, PoolRouter, Routers }
import akka.actor.typed.{ ActorRef, Behavior }

object JobWorker {

  sealed trait Command

  case class Work(
      jobName: String,
      master: ActorRef[JobMaster.Command])
      extends Command
  case class WorkLoadDepleted(jobName: String) extends Command
  case class Task(
      words: List[String],
      master: ActorRef[JobMaster.Command])
      extends Command

  def apply() =
    Behaviors.receive[Command] {
      case (context, Work(jobName, master)) =>
        context.log.debug(
          s"Enlisting, will start requesting work for job '${jobName}'.")
        master ! JobMaster.Enlist(context.self)
        master ! JobMaster.NextTask(context.self)
        enlisted(0)
      case _ => Behaviors.unhandled
    }

  def enlisted(taskProcessed: Int): Behavior[Command] =
    Behaviors.receive[Command] {
      case (context, WorkLoadDepleted(jobName)) =>
        context.log.debug(
          s"Work load '$jobName' is depleted, retiring...")
        Behaviors.same
      case (context, Task(textPart, master)) =>
        master ! JobMaster.NextTask(context.self)
        Behaviors.same
    }
}
