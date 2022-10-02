package subscriptions

import akka.actor.typed.ActorSystem

object ClusterDomainEventListenerApp extends App {

  val guardian = ActorSystem(ClusterDomainEventListener(), "words")
}


