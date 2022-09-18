package routers

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorRef, Behavior }

object Switch {

  sealed trait Command
  case object SwitchOn extends Command
  case object SwitchOff extends Command
  case class Payload(content: String, metadata: String)
      extends Command

  def apply(
      forwardTo: ActorRef[String],
      alertTo: ActorRef[String]): Behavior[Command] =
    on(forwardTo, alertTo)

  def on(
      forwardTo: ActorRef[String],
      alertTo: ActorRef[String]): Behavior[Command] =
    Behaviors.receive { (context, message) =>
      message match {
        case SwitchOn =>
          context.log.warn("sent SwitchOn but was ON already")
          Behaviors.same
        case SwitchOff =>
          off(forwardTo, alertTo)
        case Payload(content, metadata) =>
          forwardTo ! content
          Behaviors.same
      }
    }

  def off(
      forwardTo: ActorRef[String],
      alertTo: ActorRef[String]): Behavior[Command] =
    Behaviors.receive { (context, message) =>
      message match {
        case SwitchOn =>
          on(forwardTo, alertTo)
        case SwitchOff =>
          context.log.warn("sent SwitchOff but was OFF already")
          Behaviors.same
        case Payload(content, metadata) =>
          alertTo ! metadata
          Behaviors.same
      }
    }
}
