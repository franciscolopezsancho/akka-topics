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

    "the flow Director -> Master -> Worker -> Master " should {
      "send a JobRequest to the Director that produces indirectly (see test above) 100 enlisted Workers" +
      "each one of them request a NextTask to the JobMaster and it gives them Tasks until depletion" in {
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
  }
}
