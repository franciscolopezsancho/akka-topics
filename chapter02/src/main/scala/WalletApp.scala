package com.manning

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorSystem, Behavior }

object WalletApp extends App {

  val guardian: ActorSystem[Int] =
    ActorSystem(Wallet(), "hello-world")
  guardian ! 1
  guardian ! 10

  println("Press ENTER to terminate")
  scala.io.StdIn.readLine()
  guardian.terminate()

}

object Wallet {

  def apply(): Behavior[Int] =
    Behaviors.receive { (context, message) =>
      context.log.info(s"received '$message' dollar(s)")
      Behaviors.same
    }

}
