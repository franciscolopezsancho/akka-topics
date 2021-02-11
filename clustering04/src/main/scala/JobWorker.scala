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
    Behaviors.setup[Command] { context =>
      Behaviors.receiveMessage {
        case Work(jobName, master) =>
          context.log.debug(
            s"Enlisted, will start requesting work for job '${jobName}'.")
          master ! JobMaster.Enlist(context.self)
          master ! JobMaster.NextTask(context.self)
          enlisted(0)
      }

    }

  def enlisted(taskProcessed: Int): Behavior[Command] =
    Behaviors.receive[Command] {
      case (context, WorkLoadDepleted(jobName)) =>
        context.log.debug(
          s"Work load '$jobName' is depleted, retiring..."
        ) //shall I back to idle?
        Behaviors.same
      case (context, Task(textPart, master)) =>
        val countMap = processTask(textPart)
        context.log.debug(s"countMap $countMap")
        master ! JobMaster.TaskResult(countMap)
        master ! JobMaster.NextTask(context.self)
        enlisted(taskProcessed + 1)
    }

  def processTask(textPart: List[String]): Map[String, Int] = {
    textPart
      .flatMap(_.split("\\W+"))
      .foldLeft(Map.empty[String, Int]) { (mapAccumulator, word) =>
        mapAccumulator + (word -> (mapAccumulator.getOrElse(word, 0) + 1))
      }
  }
}
