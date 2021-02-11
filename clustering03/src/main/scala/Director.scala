package example

import akka.actor.typed.scaladsl.{ Behaviors, PoolRouter, Routers }
import akka.actor.typed.{ ActorRef, Behavior }

object Director {

  sealed trait Command

  case class JobRequest(name: String, texts: List[String])
      extends Command
  case class WordCount(
      jobName: String,
      wordsCountMap: Map[String, Int])
      extends Command
  case class JobSuccess(name: String, aggregate: Map[String, Int])
      extends Command //event?

  case class Job(
      name: String,
      text: List[String],
      director: ActorRef[Director.Command],
      jobMaster: ActorRef[JobMaster.Command])

  def apply(jobs: List[Job] = List()): Behavior[Command] =
    Behaviors.setup { context =>
      Behaviors.receiveMessage {
        case JobRequest(name, texts) =>
          context.log.info(s"received job $name")

          val master = context.spawn(JobMaster(), "name")
          master ! JobMaster.StartJob(name, texts, context.self)
          apply(jobs :+ Job(name, texts, context.self, master))

        case JobSuccess(name, aggregate) =>
          Behaviors.same

        case WordCount(jobName, wordsCountMap) =>
          context.log.debug(s"!!!!! ${wordsCountMap.toString}")
          Behaviors.same
      }
    }
}
