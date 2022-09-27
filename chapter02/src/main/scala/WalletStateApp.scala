package com.manning

import akka.actor.typed.ActorSystem

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
