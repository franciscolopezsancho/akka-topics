package logging.config

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorRef, Behavior }
import scala.concurrent.duration._
import akka.actor.testkit.typed.scaladsl.{
  LogCapturing,
  LoggingTestKit,
  TestProbe
}
import org.slf4j.event.Level
import com.typesafe.config.{ Config, ConfigFactory }

class AsyncLogConfigSpec
    extends ScalaTestWithActorTestKit(
      ConfigFactory
        .parseString("""app.prefix = "otherPrefix"""")
        .withFallback(ConfigFactory.load("lifting")))
    with AnyWordSpecLike
    with Matchers {

  "Actor" must {

    "log in debug the content when receiving message" in {
      val loggerBehavior: Behavior[String] = Behaviors.receive {
        (context, message) =>
          message match {
            case message: String =>
              context.log.debug(s"message '$message', received")
              Behaviors.same
          }
      }

      val loggerActor = spawn(loggerBehavior)
      val message = "hi"

      LoggingTestKit.debug(s"message '$message', received").expect {
        loggerActor.ref ! message
      }
    }

    "log the prefix from conf when receiving message" in {
      val prefix = testKit.system.settings.config
        .getString("app.prefix")
      val suffix = testKit.system.settings.config
        .getString("app.suffix")

      val loggerBehavior: Behavior[String] = Behaviors.receive {
        (context, message) =>
          message match {
            case message: String =>
              context.log.info(s"$prefix '$message' $suffix")
              Behaviors.same
          }
      }

      val loggerActor = spawn(loggerBehavior)
      val message = "hi"

      LoggingTestKit
        .info(s"otherPrefix '$message' initialSuffix")
        .expect {
          loggerActor.ref ! message
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
