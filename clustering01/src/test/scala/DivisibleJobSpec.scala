package example

import akka.actor.testkit.typed.scaladsl.{
  LogCapturing,
  LoggingTestKit,
  ScalaTestWithActorTestKit,
  TestProbe
}
import akka.actor.typed.scaladsl.{ Behaviors, PoolRouter, Routers }
import akka.actor.typed.{ ActorRef, Behavior }

import scala.concurrent.duration._

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class DivisibleJobSpec
    extends ScalaTestWithActorTestKit
    with AnyWordSpecLike
    with Matchers
    with LogCapturing {

  "The words app" should {
    "count ocurrences of each word in a list of phrases" ignore {
      val probe = TestProbe[Director.Command]
      val directorMonitored =
        spawn(Behaviors.monitor(probe.ref, Director()), "director0")
      val words =
        List("this is a test ", "this is a test", "this is", "this")
      directorMonitored ! Director.JobRequest(
        "test0",
        List.fill(100000)(words).flatten)
      probe.expectMessageType[Director.JobRequest]
      probe.expectMessage(
        Director.JobSuccess(
          "test0",
          Map(
            "this" -> 400000,
            "is" -> 300000,
            "a" -> 200000,
            "test" -> 200000)))
    }
  }
  "the flow from Director to Worker" should {
    "send a JobRequest to the Director creating a JobMaster that produces a 100 Workers" +
    "and broadcast 'Work' to each one of them so they get ready to be enlisted" in {
      val director =
        spawn(Director(), "director")

      LoggingTestKit
        .debug(
          "Enlisting, will start requesting work for job 'test2'.")
        .withOccurrences(100)
        .expect {
          director ! Director.JobRequest(
            "test2",
            List("this is a test "))
        }
    }
  }
}

object Director {

  sealed trait Command

  case class JobRequest(name: String, texts: List[String])
      extends Command
  case class JobSuccess(name: String, aggregate: Map[String, Int])
      extends Command //event?

  case class Job(
      name: String,
      text: List[String],
      director: ActorRef[Director.Command],
      jobMaster: ActorRef[JobMaster.Command])

  def apply(): Behavior[Command] =
    Behaviors.setup { context =>
      Behaviors.receiveMessage {
        case JobRequest(name, texts) =>
          context.log.info(s"received job $name")

          val master = context.spawn(JobMaster(), "name")
          master ! JobMaster.StartJob(name, texts, context.self)
          Behaviors.same

        case JobSuccess(name, aggregate) =>
          Behaviors.same

      }
    }
}

object JobMaster {

  sealed trait Command

  case class StartJob(
      name: String,
      texts: List[String],
      director: ActorRef[Director.Command])
      extends Command

  def apply() = Behaviors.setup[Command] { context =>

    val workers = context.spawn(
      Routers
        .pool(poolSize = 100)(JobWorker())
        .withBroadcastPredicate(_ => true),
      "workers-pool")

    Behaviors.receiveMessage {
      case StartJob(jobName, texts, director) =>
        workers ! JobWorker.Work(jobName, context.self)
        working(jobName, director)
    }
  }

  def working(
      jobName: String,
      director: ActorRef[Director.Command]): Behavior[Command] =
    Behaviors.same

}

object JobWorker {

  sealed trait Command

  case class Work(
      jobName: String,
      master: ActorRef[JobMaster.Command])
      extends Command

  def apply() = Behaviors.receive[Command] {
    case (context, Work(jobName, master)) =>
      context.log.debug(
        s"Enlisting, will start requesting work for job '${jobName}'.")
      Behaviors.same
  }
}
