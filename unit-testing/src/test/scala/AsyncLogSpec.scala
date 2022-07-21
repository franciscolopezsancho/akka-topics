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

  // "Actor Proxy" must {

  //   "redirect to Listener and log the content of the message" in {
  //     val proxy = testKit.spawn(Proxy(), "reading-logs")
  //     val reader = testKit.spawn(Listener(), "reader")
  //     val message = "aloha"

  //     LoggingTestKit.info(s"message '$message', received").expect {
  //       proxy ! Proxy.Send(s"$message", reader.ref)
  //     }
  //   }
  // }

  "a Simplified Manager" must {

    "be able to log 'it's done'" in {
      val manager = testKit.spawn(SimplifiedManager(), "manager")

      LoggingTestKit.info("it's done").expect {

        manager ! SimplifiedManager.Log
      }
    }
  }
}

object Proxy {

  sealed trait Message
  case class Send(message: String, sendTo: ActorRef[String])
      extends Message

  def apply(): Behavior[Message] = Behaviors.receiveMessage {
    case Send(message, sendTo) =>
      sendTo ! message
      Behaviors.same
  }
}

object Listener {

  def apply(): Behavior[String] = Behaviors.receive {
    (context, message) =>
      context.log.info(s"message '$message', received")
      Behaviors.same
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
