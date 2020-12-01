package logging.config

import akka.actor.testkit.typed.scaladsl.ActorTestKit

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorRef, Behavior }
import scala.concurrent.duration._
import akka.actor.testkit.typed.scaladsl.LoggingTestKit
import akka.actor.testkit.typed.scaladsl.TestProbe
import org.slf4j.event.Level

class AsyncLogConfigSpec
    extends ScalaTestWithActorTestKit("akka.loglevel = DEBUG")
    with AnyWordSpecLike
    with Matchers {

  "Actor" must {

    "log in debug the content when receiving message" in {
      val behavior: Behavior[String] = Behaviors.receive { (context, message) =>
        message match {
          case message: String =>
            context.log.debug(s"message '$message', received")
            Behaviors.stopped
        }
      }

      val actor = testKit.spawn(behavior)
      val message = "aloha"

      LoggingTestKit.debug(s"message '$message', received").expect {
        actor.ref ! message
      }
    }

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
