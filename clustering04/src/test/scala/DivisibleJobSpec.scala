package example

import akka.actor.testkit.typed.scaladsl.{
  FishingOutcomes,
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

  override implicit val patienceConfig: PatienceConfig =
    PatienceConfig(5.second) //? is this working?

  "The words app" should {
    "count ocurrences of each word in a list of phrases" in {
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
    "send a JobRequest to the Director creating a JobMaster that produces a 100 Workers," +
    "and broadcast 'Work' to each one of them so they get ready to be enlisted" in {
      val director =
        spawn(Director(), "director1")

      LoggingTestKit
        .debug(
          "Enlisting, will start requesting work for job 'test1'.")
        .withOccurrences(100)
        .expect {
          director ! Director.JobRequest(
            "test1",
            List("this is a test "))
        }
    }
  }

  "the flow Director -> Master -> Worker -> Master " should {
    "send a JobRequest to the Director that produces indirectly (see test above) 100 enlisted Worker," +
    "each one of them request a NextTask to the JobMaster which gives them Tasks until depletion" in {
      val director =
        spawn(Director(), "director2")

      LoggingTestKit
        .debug("Work load 'test2' is depleted, retiring...")
        .withOccurrences(100)
        .expect {
          director ! Director.JobRequest(
            "test2",
            List("this is a test "))
        }
    }
  }
  "an inermediate step, only Master -> Worker -> Master" should {
    "sending StartJob to master will in time return the words count and aggregated" in {
      val probe = TestProbe[JobMaster.Command]
      val jobMasterMonitored =
        Behaviors.monitor(probe.ref, JobMaster())
      val director =
        spawn(Director(), "director4")
      val master = spawn(jobMasterMonitored, "master4")
      master ! JobMaster.StartJob(
        "test4",
        List("there are", "are many"),
        director)

      probe.expectMessageType[JobMaster.StartJob]

      val messages = probe.fishForMessage(5.second) {
        case JobMaster.Enlist(_) =>
          FishingOutcomes.continueAndIgnore
        case JobMaster.NextTask(_) =>
          FishingOutcomes.continueAndIgnore
        case JobMaster.TaskResult(_) => FishingOutcomes.complete
        case message =>
          FishingOutcomes.fail(s"unexpected message: $message")
      }

      messages contains Seq(
        JobMaster.TaskResult(
          Map("there" -> 1, "are" -> 2, "many" -> 1)))

    }

  }

  "the accumulator" should {
    "retrieve a map " in {
      JobWorker.processTask(List("Hi there")) shouldBe Map(
        "Hi" -> 1,
        "there" -> 1)
    }
  }

}
