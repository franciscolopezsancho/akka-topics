package async

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorRef, Behavior }

class AsyncTestingExampleSpec
    extends AnyWordSpec
    with BeforeAndAfterAll
    with Matchers {

  val testKit = ActorTestKit()

  "Actor Echo" must {

    "return same string" in {

      val echo = testKit.spawn(Echo(), "Akka")
      val probe = testKit.createTestProbe[String]()
      val content = "Heellooo"
      echo ! Echo.Sound(content, probe.ref)
      probe.expectMessage(content)

    }
  }

  // override def afterAll(): Unit = testKit.shutdownTestKit()

  "Actor Proxy" must {

    "redirect to Reader the content of the messages received" in {

      val proxy = testKit.spawn(Proxy(), "short-chain")
      val reader = testKit.createTestProbe[Reader.Text]()
      proxy ! Proxy.Send("hello", reader.ref)
      reader.expectMessage(Reader.Read("hello"))
    }
  }
}

object Echo {

  case class Sound(content: String, origin: ActorRef[String])

  def apply(): Behavior[Sound] =
    Behaviors.receiveMessage { msg =>
      msg.origin ! msg.content
      Behaviors.same
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
