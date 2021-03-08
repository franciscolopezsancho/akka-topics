package time

import akka.actor.typed.{ ActorSystem, Behavior }
import akka.actor.typed.scaladsl.Behaviors
import scala.concurrent.duration._

object TimeApp extends App {

  val guardian =
    ActorSystem[Timing.Command](Timing(), "timing-example")
  guardian ! Timing.Start
}

object Timing {

  sealed trait Command

  case object Start extends Command

  private object TimedOut extends Command
  private object Checking extends Command

  private object CheckingKey

  def apply(): Behavior[Command] =
    Behaviors.receive { (context, message) =>
      Behaviors.withTimers[Command] { timers =>
        message match {
          case Start =>
            context.log.info("started")
            timers.startSingleTimer(TimedOut, 5.second)
            timers.startTimerWithFixedDelay(
              CheckingKey,
              Checking,
              1.second)
          case TimedOut =>
            context.log.info("time is out")
            timers.cancel(CheckingKey)
          case Checking =>
            context.log.info("just checking")
        }
        Behaviors.same
      }

    }

}
