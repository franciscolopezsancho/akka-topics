package fishing

import akka.actor.testkit.typed.scaladsl.{
  FishingOutcomes,
  ManualTime,
  ScalaTestWithActorTestKit,
  TestProbe
}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import akka.actor.typed.scaladsl.{ Behaviors, TimerScheduler }
import akka.actor.typed.{ ActorRef, Behavior }

import scala.concurrent.duration._

class FishingSpecManualTime
    extends ScalaTestWithActorTestKit(ManualTime.config)
    with AnyWordSpecLike
    with Matchers {

  val manualTime: ManualTime = ManualTime()

  // "A timing test 2" must {

  //   import Forward._

  //   val interval = 1.seconds

  //   "be able to cancel timer" in {
  //     val probe = TestProbe[Event]("evta")
  //     val behv = Behaviors.withTimers[Command] { timer =>
  //       timer.startTimerWithFixedDelay(TimerKey("y"), Tick(1), interval)
  //       apply(probe.ref, timer)
  //     }

  //     val ref = spawn(behv)
  //     probe.expectMessage(Tock(1))
  //     ref ! Cancel
  //     probe.fishForMessage(3.seconds) {
  //       // we don't know that we will see exactly one tock
  //       case _: Tock => FishingOutcomes.continue
  //       // but we know that after we saw Cancelled we won't see any more
  //       case Cancelled => FishingOutcomes.complete
  //       case message   => FishingOutcomes.fail(s"unexpected message: $message")
  //     }
  //     probe.expectNoMessage(interval + 100.millis)
  //   }
  // }
}
