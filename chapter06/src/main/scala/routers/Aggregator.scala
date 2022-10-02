package routers

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.receptionist.ServiceKey
import akka.actor.typed.scaladsl.Behaviors

object Aggregator {

  val serviceKey = ServiceKey[Aggregator.Command]("agg-key")

  def mapping(command: Command) = command.id

  sealed trait Command {
    def id: String
  }
  final case class Obfuscated(id: String, content: String)
      extends Command
  final case class Enriched(id: String, metadata: String)
      extends Command

  sealed trait Event{
    def id: String
  }
  final case class Completed(
      id: String,
      content: String,
      metadata: String)
      extends Event

  def apply(
      messages: Map[String, String] = Map(),
      forwardTo: ActorRef[Event]): Behavior[Command] =
    Behaviors.setup[Command] { context =>

      context.system.receptionist ! Receptionist.Register(
        serviceKey,
        context.self)

      Behaviors.receiveMessage {
        case v @ Obfuscated(id, content) =>
          messages.get(id) match {
            case Some(metadata) =>
              forwardTo ! Completed(id, content, metadata)
              apply(messages - (id, metadata), forwardTo)
            case None =>
              apply(messages + (id -> content), forwardTo)
          }
        case e @ Enriched(id, metadata) =>
          messages.get(id) match {
            case Some(content) =>
              forwardTo ! Completed(id, content, metadata)
              apply(messages - (id, content), forwardTo)
            case None =>
              apply(messages + (id -> metadata), forwardTo)
          }
      }
    }
}
