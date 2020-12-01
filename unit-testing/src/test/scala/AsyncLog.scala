package logging

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

class AsyncTestingExampleSpec
    extends ScalaTestWithActorTestKit
    with AnyWordSpecLike
    with Matchers {

  "Actor Proxy" must {

    "redirect to Reader and log the content of the message" in {
      val proxy = testKit.spawn(Proxy(), "reading-logs")
      val reader = testKit.spawn(Reader(), "reader")
      val message = "aloha"

      LoggingTestKit.info(s"message '$message', received").expect {
        proxy ! Proxy.Send(s"$message", reader.ref)
      }
    }
  }
}

object Proxy {

  sealed trait Message
  case class Send(message: String, sendTo: ActorRef[Reader.Text])
      extends Message

  def apply(): Behavior[Message] = Behaviors.receiveMessage {
    case Send(message, sendTo) =>
      sendTo ! Reader.Read(message)
      Behaviors.same
  }
}

object Reader {

  sealed trait Text
  case class Read(message: String) extends Text

  def apply(): Behavior[Text] = Behaviors.receive { (context, message) =>
    message match {
      case Read(message) =>
        context.log.info(s"message '$message', received")
        Behaviors.stopped
    }
  }
}
