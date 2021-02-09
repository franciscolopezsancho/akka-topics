package example

import akka.actor.testkit.typed.scaladsl.{
  LogCapturing,
  ScalaTestWithActorTestKit,
  TestProbe
}
import akka.actor.typed.scaladsl.Behaviors

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class DivisibleJobSpec
    extends ScalaTestWithActorTestKit
    with AnyWordSpecLike
    with Matchers
    with LogCapturing {

  "The words app" should {
    "count the words ocurrences in a text" in {
      val testProbe = TestProbe[JobManager.Command]
      val jobManagerWithMonitor =
        spawn(
          Behaviors.monitor(testProbe.ref, JobManager()),
          "manager")
      jobManagerWithMonitor ! JobManager.JobRequest(
        "test2",
        List("this is a test ", "this is a test", "this is", "this"))
      testProbe.expectMessage(
        JobManager.JobSuccess(
          "test2",
          Map("this" -> 4, "is" -> 3, "a" -> 2, "test" -> 2)))
      testProbe.expectNoMessage
    }
  }

}

object JobManager {

  sealed trait Command

  case class JobRequest(name: String, texts: List[String])
      extends Command
  case class JobSuccess(name: String, aggregate: Map[String, Int])
      extends Command

  def apply() = Behaviors.setup[Command] { context =>
    Behaviors.receiveMessage {
      case JobRequest(name, texts) =>
        Behaviors.same

      case JobSuccess(name, aggregate) =>
        Behaviors.same

    }
  }
}
