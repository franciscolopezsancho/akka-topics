package example

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

object Cap {

  final case class Increment(increment: Int, replyTo: ActorRef[Int])

  def apply(current: Int, max: Int): Behavior[Increment] = {
    Behaviors.receiveMessage { message =>
      message match {
        case Increment(increment, replyTo) =>
          if (current + increment > max) {
            replyTo ! current
            Behaviors.same
          } else {
            replyTo ! current + increment
            apply(current + increment, max)
          }
      }
    }
  }
}
