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

  "The words app" should {
    "count the words ocurrences in a text" ignore {
      val probe = TestProbe[Director.Command]
      val directorMonitored =
        spawn(Behaviors.monitor(probe.ref, Director()), "director")
      directorMonitored ! Director.JobRequest(
        "test0",
        List("this is a test ", "this is a test", "this is", "this"))
      probe.expectMessageType[Director.JobRequest]
      probe.expectMessageType[Director.WordCount]
      probe.expectMessage(
        Director.JobSuccess(
          "test0",
          Map("this" -> 4, "is" -> 3, "a" -> 2, "test" -> 2)))
      probe.expectNoMessage
    }
    "the basic flow" should {
      "send a JobRequest to the Director that produces a Worker that receives it" ignore {
        val director =
          spawn(Director(), "director")

        LoggingTestKit
          .debug(
            "Enlisted, will start requesting work for job 'test'.")
          .withOccurrences(100)
          .expect {
            director ! Director.JobRequest(
              "test",
              List("this is a test "))
          }
      }
      "send a JobRequest to the Director that produces a Worker to respond to master" ignore {
        val director =
          spawn(Director(), "director-2")

        LoggingTestKit
          .debug(
            "Enlisted, will start requesting work for job 'test2'.")
          .withOccurrences(100)
          .expect {
            director ! Director.JobRequest(
              "test2",
              List("this is a test "))
          }
      }
    }
    "the second flow" should {
      "send a JobRequest to the Director that produces a Worker that receives it" +
      " then the Worker send back request for a task to Manager and the Worker receives tasks till not task is left but it doesn't perform the task only consumes it" ignore {
        val director =
          spawn(Director(), "director3")

        LoggingTestKit
          .debug("Work load 'test3' is depleted, retiring...")
          .withOccurrences(100)
          .expect {
            director ! Director.JobRequest(
              "test3",
              List("this is a test "))
          }
      }
    }
    "the thirds flow" should { //should I try composition from lower level, meaning not starting the flow from the Director?
      "send a JobRequest to the Director that produces a Worker that receives it" +
      " then the Worker send back request for a task to Manager and the Worker receives tasks till not task is left performing the task and informing the Manager" +
      " then the Manager should receive MergeResults" in {
        val probe = TestProbe[Director.Command]
        val directorMonitored =
          Behaviors.monitor(probe.ref, Director())
        val director =
          spawn(directorMonitored, "director4")

        director ! Director.JobRequest(
          "test4",
          List("this is a test ", "it is"))
        probe.expectMessageType[Director.JobRequest]
        val wordCount =
          probe.receiveMessage().asInstanceOf[Director.WordCount]

        wordCount.wordsCountMap.toSet contains
        Set(("test", 1), ("this", 1), ("is", 1), ("a", 1), ("it", 1))

      }

      "a master starting a job should receive a task result with the totals" in {
        val probe = TestProbe[JobMaster.Command]
        val jobMasterMonitored =
          Behaviors.monitor(probe.ref, JobMaster())
        val director =
          spawn(Director(), "director5")
        val master = spawn(jobMasterMonitored, "master-1")
        master ! JobMaster.StartJob(
          "test5",
          List("there are", "are many"),
          director)

        probe.expectMessageType[JobMaster.StartJob]

        val messages = probe.fishForMessage(5.second) {
          case JobMaster.Enlist(_) =>
            FishingOutcomes.continueAndIgnore
          case JobMaster.NextTask(_) =>
            FishingOutcomes.continueAndIgnore
          case JobMaster.TaskResult(_) => FishingOutcomes.continue
          case JobMaster.MergeResults  => FishingOutcomes.complete
          case message =>
            FishingOutcomes.fail(s"unexpected message: $message")
        }

        messages contains Seq(
          JobMaster.TaskResult(
            Map("there" -> 1, "are" -> 2, "many" -> 1)),
          JobMaster.MergeResults)

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

}
