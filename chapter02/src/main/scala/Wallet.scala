package com.manning

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.Behavior

object Wallet {

  def apply(): Behavior[Int] =
    Behaviors.receive { (context, message) =>
      context.log.info(s"received '$message' dollar(s)")
      Behaviors.same
    }

}
