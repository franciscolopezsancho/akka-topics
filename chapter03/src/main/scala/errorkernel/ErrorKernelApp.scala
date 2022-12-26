package errorkernel

import akka.actor.typed.ActorSystem

object ErrorKernelApp extends App {

  val guardian: ActorSystem[Guardian.Command] =
    ActorSystem(Guardian(), "error-kernel")
  guardian ! Guardian.Start(List("-one-", "--two--"))

  println("press ENTER to terminate")
  scala.io.StdIn.readLine()
  guardian.terminate()
}
