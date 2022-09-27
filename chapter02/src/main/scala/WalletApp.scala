package com.manning

import akka.actor.typed.ActorSystem

object WalletApp extends App {

  val guardian: ActorSystem[Int] =
    ActorSystem(Wallet(), "wallet")
  guardian ! 1
  guardian ! 10

  println("Press ENTER to terminate")
  scala.io.StdIn.readLine()
  guardian.terminate()

}
