package asyncclock

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.testkit.typed.scaladsl.ManualTime
import akka.actor.testkit.typed.scaladsl.TestProbe

import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorRef, Behavior }
import scala.concurrent.duration._

class AsyncClockSpec
    extends ScalaTestWithActorTestKit(ManualTime.config)
    with AnyWordSpecLike
    with Matchers {

  val manualTime: ManualTime = ManualTime()

  "Manual Time" must {

    "redirect to Reader the content of the messages received" in {
      val exchanger = spawn(SwapDelayed(), "cookie") // create -> monitor
      val probe = TestProbe[String]("counter")
      exchanger ! SwapDelayed.Take(probe.ref, "stuff")
      manualTime.expectNoMessageFor(99.millis, probe)
      manualTime.timePasses(2.millis)
      probe.expectMessage("stuff".reverse)
      manualTime.expectNoMessageFor(1.seconds, probe)

    }
  }
}

// def tap[T: ClassTag] = new BehaviorInterceptor[T, T] {
//     override def aroundReceive(context: TypedActorContext[T], message: T, target: ReceiveTarget[T]): Behavior[T] =
//       target(context, message)
// }

object SwapDelayed {

  sealed trait Command
  case class Take(from: ActorRef[String], thing: String)
      extends Command
  case class Give(to: ActorRef[String], thing: String) extends Command

  def apply(): Behavior[Command] =
    Behaviors.withTimers { timers =>
      Behaviors.receiveMessage {
        case Take(from, thing) =>
          timers.startSingleTimer(
            "unusedKey",
            Give(from, thing),
            100.millis)
          Behaviors.same
        case Give(to, thing) =>
          to ! thing.reverse
          Behaviors.same
      }
    }
}

object Counter {

  sealed trait Command
  case object Increase extends Command
  case object CancelTimer extends Command

  private object IncreaseKey

  def apply(count: Int): Behavior[Command] =
    Behaviors.withTimers { timers =>
      timers.startPeriodicTimer(IncreaseKey, Increase, 10.millis)
      Behaviors.receive { (context, message) =>
        message match {
          case Increase =>
            context.log.info(s"count '$count'")
            apply(count + 1)
          case CancelTimer =>
            timers.cancel(IncreaseKey)
            Behaviors.same

        }
      }
    }
}
