package routers

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

object Worker {
  def apply(monitor: ActorRef[String]): Behavior[String] =
    Behaviors.receiveMessage[String] {
      case message =>
        monitor ! message
        Behaviors.same
    }
}
