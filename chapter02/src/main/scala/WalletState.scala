package com.manning

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

object WalletState {

  sealed trait Command
  final case class Increase(amount: Int) extends Command
  final case class Decrease(amount: Int) extends Command

  def apply(total: Int, max: Int): Behavior[Command] =
    Behaviors.receive { (context, message) =>
      message match {
        case Increase(amount) =>
          val current = total + amount
          if (current <= max) {
            context.log.info(s"increasing to $current")
            apply(current, max)
          } else {
            context.log.info(
              s"I'm overloaded. Counting '$current' while max is '$max. Stopping")
            Behaviors.stopped
          }
        case Decrease(amount) =>
          val current = total - amount
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
