package basics

import akka.actor.testkit.typed.scaladsl.BehaviorTestKit
import akka.actor.testkit.typed.scaladsl.TestInbox
import akka.actor.testkit.typed.CapturedLogEvent
import akka.actor.testkit.typed.Effect.{
  NoEffects,
  Scheduled,
  Spawned,
  Stopped
}
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.exceptions.TestFailedException
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.ActorRef
import org.slf4j.event.Level
import scala.concurrent.duration.DurationInt

class SyncTestingSpec extends AnyWordSpec with Matchers {

  "Typed actor synchronous testing" must {

    "spawning takes place" in {
      val testKit = BehaviorTestKit(SimplifiedManager())
      testKit.expectEffect(NoEffects)
      testKit.run(SimplifiedManager.CreateChild("adan"))
      testKit.expectEffect(Spawned(SimplifiedWorker(), "adan"))
    }

    "actor gets forwarded message from manager" in {
      val testKit = BehaviorTestKit(SimplifiedManager())
      val probe = TestInbox[String]()
      testKit.run(
        SimplifiedManager.Forward("message-to-parse", probe.ref))
      probe.expectMessage("message-to-parse")
      probe.hasMessages shouldBe false
    }

    "record the log" in {
      val testKit = BehaviorTestKit(SimplifiedManager())
      testKit.run(SimplifiedManager.Log)
      testKit.logEntries() shouldBe Seq(
        CapturedLogEvent(Level.INFO, "it's done"))
    }

    "failing to schedule a message. BehaviorTestKit can't deal with scheduling" in {
      intercept[TestFailedException] {
        val testKit = BehaviorTestKit(SimplifiedManager())
        testKit.run(SimplifiedManager.ScheduleLog)
        testKit.expectEffect(
          Scheduled(1.seconds, testKit.ref, SimplifiedManager.Log))
        testKit.logEntries() shouldBe Seq(
          CapturedLogEvent(Level.INFO, "it's done"))
      }
    }
  }
}

object SimplifiedWorker {
  def apply() = Behaviors.ignore[String]
}

object SimplifiedManager {

  sealed trait Command
  final case class CreateChild(name: String) extends Command
  final case class Forward(message: String, sendTo: ActorRef[String])
      extends Command
  final case object ScheduleLog extends Command
  final case object Log extends Command

  def apply(): Behaviors.Receive[Command] =
    Behaviors.receive { (context, message) =>
      message match {
        case CreateChild(name) =>
          context.spawn(SimplifiedWorker(), name)
          Behaviors.same
        case Forward(text, sendTo) =>
          sendTo ! text
          Behaviors.same
        case ScheduleLog =>
          context.scheduleOnce(1.seconds, context.self, Log)
          Behaviors.same
        case Log =>
          context.log.info(s"it's done")
          Behaviors.same
      }
    }
}
