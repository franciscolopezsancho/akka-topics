package logging

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.testkit.typed.scaladsl.LoggingTestKit
import akka.actor.testkit.typed.scaladsl.LogCapturing

import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorRef, Behavior }
import scala.concurrent.duration._

class AsyncLogSpec
    extends ScalaTestWithActorTestKit
    with AnyWordSpecLike
    with Matchers {

  "a monitor" must {

    "intercept the messages" in {

      val probe = createTestProbe[String]
      val behaviorUnderTest = Behaviors.receiveMessage[String] { _ =>
        Behaviors.ignore
      }
      val behaviorMonitored =
        Behaviors.monitor(probe.ref, behaviorUnderTest)
      val actor = testkit.spawn(behaviorMonitored)

      actor ! "checking"
      probe.expectMessage("checking")

    }
  }

  "a simple behavior" must {

    "log messages to dead letters" in {

      val behavior: Behavior[String] =
        Behaviors.stopped

      val carl = spawn(behavior, "carl")

      LoggingTestKit.empty
        .withLogLevel(Level.INFO)
        .withMessageRegex(
          ".*Message.*to.*carl.*was not delivered.*2.*dead letters encountered")
        .expect {
          carl ! "Hello"
          carl ! "Hello"
        }
    }
  }
}

object SimplifiedManager {

  sealed trait Command
  case object Log extends Command

  def apply(): Behaviors.Receive[Command] =
    Behaviors.receive { (context, message) =>
      message match {
        case Log =>
          context.log.info(s"it's done")
          Behaviors.same
      }
    }

}
