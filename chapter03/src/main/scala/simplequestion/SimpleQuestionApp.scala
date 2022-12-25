package simplequestion

import akka.actor.typed.ActorSystem

object SimpleQuestionApp extends App {

  val guardian: ActorSystem[Guardian.Command] =
    ActorSystem(Guardian(), "example-ask-without-content")
  guardian ! Guardian.Start(List("text-a", "text-b", "text-c"))

  println("press ENTER to terminate")
  scala.io.StdIn.readLine()
  guardian.terminate()
}
