package faulttolerance2
//SECOND DESIGN. Figure 5.13 and 5.14

import akka.actor.typed.ActorSystem

object LogProcessingApp extends App {
  //this directories may come from settings
  //or from args using Main instead of extending from 'App'
  val directories = Vector("file:///source1/", "file:///source2/")

  val guardian = ActorSystem[Nothing](
    LogProcessingGuardian(directories),
    "log-processing-app")
}
