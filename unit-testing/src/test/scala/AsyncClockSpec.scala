package asyncclock

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.testkit.typed.scaladsl.ManualTime

import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorRef, Behavior }
import scala.concurrent.duration._
import akka.actor.testkit.typed.scaladsl.TestProbe

class AsyncClockSpec
    extends ScalaTestWithActorTestKit(ManualTime.config)
    with AnyWordSpecLike
    with Matchers {

  val manualTime: ManualTime = ManualTime()

  "Actor Proxy" must {

    "redirect to Reader the content of the messages received" ignore {
      val probe = TestProbe[Counter.Command]("counter")
      probe.ref ! Counter.Increase
      probe.expectMessage(Counter.Increase)
      manualTime.expectNoMessageFor(9.millis, probe)
      manualTime.timePasses(21.millis)
      probe.expectMessage(Counter.Increase)
      manualTime.timePasses(2.millis)

      probe.ref ! Counter.CancelTimer
      manualTime.expectNoMessageFor(9.millis, probe)

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
