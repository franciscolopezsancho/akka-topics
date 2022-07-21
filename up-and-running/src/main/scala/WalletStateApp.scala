package com.manning

import akka.actor.typed.{ ActorSystem, Behavior }
import akka.actor.typed.scaladsl.Behaviors

object WalletState {

  sealed trait Command
  final case class Increase(dollars: Int) extends Command
  final case class Decrease(dollars: Int) extends Command

  def apply(count: Int, max: Int): Behavior[Command] =
    Behaviors.receive { (context, message) =>
      message match {
        case Increase(dollars) =>
          val current = count + dollars
          if (current <= max) {
            context.log.info(s"increasing to $current")
            apply(current, max)
          } else {
            context.log.info(
              s"I'm overloaded. Counting '$current' while max is '$max. Stopping")
            Behaviors.stopped
          }
        case Decrease(dollars) =>
          val current = count - dollars
          if (current < 0) {
            context.log.info("Can't run below zero. Stopping.")
            Behaviors.stopped
          } else {
            context.log.info(s"decreasing to $current")
            apply(current, max)
          }
      }
    }
}

object WalletStateApp extends App {

  val guardian: ActorSystem[WalletState.Command] =
    ActorSystem(WalletState(0, 2), "wallet-state")
  guardian ! WalletState.Increase(1)
  guardian ! WalletState.Increase(1)
  guardian ! WalletState.Increase(1)

  println("Press ENTER to terminate")
  scala.io.StdIn.readLine()
  guardian.terminate()

}
