package fishing

import akka.actor.typed.{ ActorSystem, Behavior }
import akka.actor.typed.scaladsl.Behaviors
import scala.concurrent.duration.DurationInt

object CounterTimer {

  sealed trait Command
  final case object Increase extends Command
  final case class Pause(seconds: Int) extends Command
  private[fishing] final case object Resume extends Command

  def apply(): Behavior[Command] =
    resume(0)

  def resume(count: Int): Behavior[Command] =
    Behaviors.receive { (context, message) =>
      Behaviors.withTimers { timers =>
        message match {
          case Increase =>
            val current = count + 1
            context.log.info(s"increasing to $current")
            resume(current)
          case Pause(t) =>
            timers.startSingleTimer(Resume, t.second)
            pause(count)
          case Resume =>
            Behaviors.same
        }
      }
    }

  def pause(count: Int): Behavior[Command] = {
    Behaviors.receive { (context, message) =>
      message match {
        case Increase =>
          context.log.info(s"counter is paused. Can't increase")
          Behaviors.same
        case Pause(t) =>
          context.log.info(s"counter is paused. Can't pause again")
          Behaviors.same
        case Resume =>
          context.log.info(s"resuming")
          resume(count)
      }
    }
  }
}
