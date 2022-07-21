package state

import akka.actor.typed.{ ActorSystem, Behavior }
import akka.actor.typed.scaladsl.Behaviors
import scala.concurrent.duration.DurationInt

object WalletTimer {

  sealed trait Command
  final case object Increase(dollars: Int) extends Command
  final case class Deactivate(seconds: Int) extends Command
  private final case object Activate extends Command

  def apply(): Behavior[Command] =
    activated(0)

  def activated(total: Int): Behavior[Command] =
    Behaviors.receive { (context, message) =>
      Behaviors.withTimers { timers =>
        message match {
          case Increase(dollars) =>
            val current = total + dollars
            context.log.info(s"increasing to $current")
            activated(current)
          case Deactivate(t) =>
            timers.startSingleTimer(Activate, t.second)
            deactivated(total)
          case Activate =>
            Behaviors.same
        }
      }
    }

  def deactivated(total: Int): Behavior[Command] = {
    Behaviors.receive { (context, message) =>
      message match {
        case Increase =>
          context.log.info(s"wallet is deactivated. Can't increase")
          Behaviors.same
        case Deactivate(t) =>
          context.log.info(s"wallet is deactivated. Can't be deactivated again")
          Behaviors.same
        case Activate =>
          context.log.info(s"activating")
          activated(total)
      }
    }
  }
}

object WalletTimerApp extends App {

  val guardian: ActorSystem[WalletTimer.Command] =
    ActorSystem(WalletTimer(), "wallet-activated")
  guardian ! WalletTimer.Increase
  guardian ! WalletTimer.Deactivate(3)

  println("Press ENTER to terminate")
  scala.io.StdIn.readLine()
  guardian.terminate()

}
