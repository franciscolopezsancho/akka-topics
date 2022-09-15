package com.manning

import akka.actor.typed.{ ActorSystem, Behavior }
import akka.actor.typed.scaladsl.Behaviors

object WalletOnOff {

  sealed trait Command
  final case class Increase(currency: Int) extends Command
  final case object Deactivate extends Command
  final case object Activate extends Command

  def apply(): Behavior[Command] =
    activated(0)

  def activated(total: Int): Behavior[Command] =
    Behaviors.receive { (context, message) =>
      message match {
        case Increase(currency) =>
          val current = total + currency
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
        case Increase(_) =>
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

  val guardian: ActorSystem[WalletOnOff.Command] =
    ActorSystem(WalletOnOff(), "wallet-on-off")
  guardian ! WalletOnOff.Increase(1)
  guardian ! WalletOnOff.Deactivate
  guardian ! WalletOnOff.Increase(1)
  guardian ! WalletOnOff.Activate
  guardian ! WalletOnOff.Increase(1)

  println("Press ENTER to terminate")
  scala.io.StdIn.readLine()
  guardian.terminate()

}
