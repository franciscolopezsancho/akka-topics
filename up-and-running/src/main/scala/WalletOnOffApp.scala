package state

import akka.actor.typed.{ ActorSystem, Behavior }
import akka.actor.typed.scaladsl.Behaviors

object WalletOnOff {

  sealed trait Command
  final case class Increase(dollars: Int) extends Command
  final case object Deactivate extends Command
  final case object Activate extends Command

  def apply(): Behavior[Command] =
    activated(0)

  def activated(total: Int): Behavior[Command] =
    Behaviors.receive { (context, message) =>
      message match {
        case Increase(dollars) =>
          val current = total + dollars
          context.log.info(s"increasing to $current")
          activated(current)
        case Deactivate =>
          deactivated(total)
        case Activate =>
          Behaviors.same
      }
    }

  def deactivated(total: Int): Behavior[Command] = {
    Behaviors.receive { (context, message) =>
      message match {
        case Increase =>
          context.log.info(s"wallet is deactivated. Can't increase")
          Behaviors.same
        case Deactivate =>
          Behaviors.same
        case Activate =>
          context.log.info(s"activating")
          activated(total)
      }
    }
  }
}

object WalletOnOffApp extends App {

  val guardian: ActorSystem[WalletActivate.Command] =
    ActorSystem(WalletActivate(), "wallet-on-off")
  guardian ! WalletActivate.Increase(1)
  guardian ! WalletActivate.Deactivate
  guardian ! WalletActivate.Increase(1)
  guardian ! WalletActivate.Activate
  guardian ! WalletActivate.Increase(1)

  println("Press ENTER to terminate")
  scala.io.StdIn.readLine()
  guardian.terminate()

}
