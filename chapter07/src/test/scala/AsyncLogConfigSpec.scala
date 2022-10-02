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
        .parseString("""akka.eventsourced-entity.journal-enabled  = false""")
        .withFallback(ConfigFactory.load("in-memory")))
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

    "lift one property from conf" in {
      val inmemory = testKit.system.settings.config
      val journalenabled =
        inmemory.getString("akka.eventsourced-entity.journal-enabled")
      val readjournal =
        inmemory.getString("akka.eventsourced-entity.read-journal")

      val loggerBehavior: Behavior[String] = Behaviors.receive {
        (context, message) =>
          message match {
            case message: String =>
              context.log.info(s"$journalenabled $readjournal")
              Behaviors.same
          }
      }

      val loggerActor = spawn(loggerBehavior)
      val message = "anymessage"

      LoggingTestKit
        .info("false inmem-read-journal")
        .expect {
          loggerActor.ref ! message
        }
    }

  }
}
