package async

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorRef, Behavior }
import scala.concurrent.duration._
import akka.actor.testkit.typed.scaladsl.FishingOutcomes
import java.util.Base64

class AsyncTestingExampleSpec
    extends AnyWordSpec
    with BeforeAndAfterAll
    with Matchers {

  val testKit = ActorTestKit()

  "Actor Encoder" must {

    "return same string" in {

      val echo = testKit.spawn(Encoder(), "Akka")
      val probe = testKit.createTestProbe[String]()
      val content = "Heellooo"
      echo ! Encoder.Encode(content, probe.ref)
      val encoded =
        java.util.Base64.getEncoder().encode(content.getBytes)
      probe.expectMessage(new String(encoded))

    }
  }

  "A Counter" must {

    "Increase its value" in {
      val counter = testKit.spawn(Counter(0), "counter")
      val probe = testKit.createTestProbe[Counter.State]()
      counter ! Counter.Increase
      counter ! Counter.GetState(probe.ref)
      probe.expectMessage(Counter.State(1))
    }
  }

  "An AdditionProxy" must {
    "be able to delegate into the Counter" in {
      val addition = testKit.spawn(AdditionProxy(), "addition-1")
      val probe = testKit.createTestProbe[AdditionProxy.Event]()
      addition ! AdditionProxy.Add(List(1, 2, 0))
      addition ! AdditionProxy.GetState(probe.ref)

      probe.expectMessage(AdditionProxy.State(3))

    }
  }

  "Actor Proxy" must {

    "redirect to Reader the content of the messages received" in {

      val proxy = testKit.spawn(Proxy(), "short-chain")
      val reader = testKit.createTestProbe[Reader.Text]()
      proxy ! Proxy.Send("hello", reader.ref)
      reader.expectMessage(Reader.Read("hello"))
    }
  }
}

import java.util.Base64

object Encoder {

  case class Encode(content: String, sendTo: ActorRef[String])

  def apply(): Behavior[Encode] =
    Behaviors.receiveMessage { sound =>
      val encoded = Base64.getEncoder().encode(sound.content.getBytes)
      sound.sendTo ! new String(encoded)
      Behaviors.same
    }
}

object Counter {

  sealed trait Command
  case class GetState(replyTo: ActorRef[State]) extends Command
  case object Increase extends Command

  case class State(count: Int)

  def apply(count: Int): Behavior[Command] =
    Behaviors.receiveMessage {
      case GetState(replyTo) =>
        replyTo ! State(count)
        Behaviors.same
      case Increase =>
        apply(count + 1)
    }
}

object AdditionProxy {

  sealed trait Command
  case class Add(numbers: List[Int]) extends Command
  case class GetState(replyTo: ActorRef[AdditionProxy.Event])
      extends Command

  sealed trait Event
  case class State(count: Int) extends Event

  private case class AdaptState(
      replyTo: ActorRef[AdditionProxy.Event],
      state: Counter.State)
      extends Command

  def apply(): Behavior[Command] =
    Behaviors.setup { context =>

      def messageAdapter(replyTo: ActorRef[AdditionProxy.Event])
          : ActorRef[Counter.State] =
        context.messageAdapter(rsp => AdaptState(replyTo, rsp))

      val counter = context.spawnAnonymous(Counter(0))

      Behaviors.receiveMessage {
        case Add(numbers) =>
          numbers.map { num =>
            for (i <- 1 to num) counter ! Counter.Increase
          }
          Behaviors.same

        case GetState(replyTo) =>
          counter ! Counter.GetState(messageAdapter(replyTo))
          Behaviors.same

        case AdaptState(replyTo, state) =>
          state match {
            case Counter.State(count) =>
              replyTo ! AdditionProxy.State(count)
          }
          Behaviors.same
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

  def apply(): Behavior[Text] = Behaviors.receive {
    (context, message) =>
      message match {
        case Read(message) =>
          context.log.info(s"message '$message', received")
          Behaviors.stopped
      }
  }
}
