import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.SupervisorStrategy

import akka.actor.testkit.typed.scaladsl.{
  LogCapturing,
  LoggingTestKit,
  ScalaTestWithActorTestKit
}

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class SupervisionExampleSpec
    extends ScalaTestWithActorTestKit(
      "akka.jvm-exit-on-fatal-error = off")
    with AnyWordSpecLike
    with Matchers
    with LogCapturing {

  "An Actor expecting a secret" must {
    "will log, throw an Exception, receive a PostStop signal" in {
      val behavior = spawn(SupervisionExample())
      LoggingTestKit.info("recoverable").expect {
        behavior ! "recoverable"
      }
    }
  }
  "will log, stop and receive a PostStop signal" in {
    val behavior = spawn(SupervisionExample())
    LoggingTestKit.info("cleaning resources").expect {
      LoggingTestKit.info("stopping").expect {
        behavior ! "stop"
      }
    }
  }
  "will grant and log" in {
    val behavior = spawn(SupervisionExample())
    LoggingTestKit.info("granted").expect {
      behavior ! "secret"
    }
  }

  //This test is last because it will kill the Actor system
  // of the ScalaTestWithActorTestKit
  "will log, throw an Exception and stop the Actor system" in {
    // because akka.jvm-exit-on-fatal-error = false
    // otherwise it will stop the jvm
    val behavior = spawn(SupervisionExample())

    behavior ! "fatal"

  }
}
