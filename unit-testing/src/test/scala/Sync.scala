package basics

import akka.actor.testkit.typed.scaladsl.BehaviorTestKit
import akka.actor.testkit.typed.scaladsl.TestInbox
import akka.actor.testkit.typed.CapturedLogEvent
import akka.actor.testkit.typed.Effect.{ NoEffects, Scheduled, Spawned }
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.exceptions.TestFailedException
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.ActorRef
import org.slf4j.event.Level
import scala.concurrent.duration.DurationInt

object SyncTestingExampleSpec {

  val dullActor = Behaviors.receiveMessage[String] { _ =>
    Behaviors.same[String]
  }

  object Hello {

    sealed trait Command
    case class Create(name: String) extends Command
    case class Proxy(message: String, sendTo: ActorRef[String]) extends Command
    case object ScheduleDone extends Command
    case object Done extends Command

    def apply(): Behaviors.Receive[Command] =
      Behaviors.receive { (context, message) =>
        message match {
          case Create(name) =>
            context.spawn(dullActor, name)
            Behaviors.same
          case Proxy(message, sendTo) =>
            sendTo ! message
            Behaviors.same
          case ScheduleDone =>
            context.scheduleOnce(1.seconds, context.self, Done)
            Behaviors.same
          case Done =>
            context.log.info(s"it's done")
            Behaviors.same
        }
      }
  }
}

class SyncTestingExampleSpec extends AnyWordSpec with Matchers {

  import SyncTestingExampleSpec._

  "Typed actor synchronous testing" must {

    "record spawning" in {
      val testKit = BehaviorTestKit(Hello())
      testKit.expectEffect(NoEffects)
      testKit.run(Hello.Create("child"))
      testKit.expectEffect(Spawned(dullActor, "child"))
    }

    "child 'dullActor' received a message" in {
      val testKit = BehaviorTestKit(Hello())
      val probe = TestInbox[String]()
      testKit.run(Hello.Proxy("hello", probe.ref))
      probe.expectMessage("hello")
      probe.hasMessages shouldBe false
    }

    "record the log" in {
      val testKit = BehaviorTestKit(Hello())
      testKit.run(Hello.Done)
      testKit.logEntries() shouldBe Seq(
        CapturedLogEvent(Level.INFO, "it's done"))
    }

    "failing to schedule a message" in {
      intercept[TestFailedException] {
        val testKit = BehaviorTestKit(Hello())
        testKit.run(Hello.ScheduleDone)
        testKit.expectEffect(Scheduled(1.seconds, testKit.ref, Hello.Done))
        testKit.logEntries() shouldBe Seq(
          CapturedLogEvent(Level.INFO, "it's done"))
      }
    }
  }
}
