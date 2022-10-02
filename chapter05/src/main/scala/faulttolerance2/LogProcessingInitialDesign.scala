package faulttolerance2
//INITIAL DESIGN. Figures 5.11 and 5.12

import akka.actor.typed.ActorSystem

object LogProcessingApp extends App {
  val sources = Vector("file:///source1/", "file:///source2/")
  val databaseUrl = "http://mydatabase1"

  val guardian = ActorSystem[Nothing](
    LogProcessingGuardian(sources, databaseUrl),
    "log-processing-app")
}
