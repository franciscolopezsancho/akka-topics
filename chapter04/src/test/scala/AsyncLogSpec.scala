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

import org.slf4j.event.Level

class AsyncLogSpec
    extends ScalaTestWithActorTestKit
    with AnyWordSpecLike
    with Matchers {

  "a Simplified Manager" must {

    "be able to log 'it's done'" in {
      val manager = testKit.spawn(SimplifiedManager(), "manager")

      LoggingTestKit.info("it's done").expect {
        manager ! SimplifiedManager.Log
      }
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
  final case object Log extends Command

  def apply(): Behaviors.Receive[Command] =
    Behaviors.receive { (context, message) =>
      message match {
        case Log =>
          context.log.info(s"it's done")
          Behaviors.same
      }
    }

}
