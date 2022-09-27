package com.manning

import akka.actor.typed.ActorSystem

object WalletTimerApp extends App {

  val guardian: ActorSystem[WalletTimer.Command] =
    ActorSystem(WalletTimer(), "wallet-timer")
  guardian ! WalletTimer.Increase(1)
  guardian ! WalletTimer.Deactivate(3)

  println("Press ENTER to terminate")
  scala.io.StdIn.readLine()
  guardian.terminate()

}
