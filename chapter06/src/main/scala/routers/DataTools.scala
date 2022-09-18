package routers

import akka.actor.typed.scaladsl.{ Behaviors, Routers }

import akka.actor.typed.{ ActorRef, Behavior }
import akka.actor.typed.receptionist.{ Receptionist, ServiceKey }

object DataObfuscator {
  sealed trait Command
  case class Message(id: String, content: String) extends Command

  def apply() = Behaviors.setup[Command] { context =>

    val router = context.spawnAnonymous {
      Routers
        .group(Aggregator.serviceKey)
        .withConsistentHashingRouting(10, Aggregator.mapping)
    }

    Behaviors.receiveMessage[Command] {
      case Message(id, content) =>
        //obfuscates sensitive data and
        router ! Aggregator
          .Obfuscated(id, content.toLowerCase)
        Behaviors.same
    }
  }
}

object DataEnricher {
  sealed trait Command
  case class Message(id: String, content: String) extends Command

  def apply() = Behaviors.setup[Command] { context =>

    val router = context.spawnAnonymous {
      Routers
        .group(Aggregator.serviceKey)
        .withConsistentHashingRouting(10, Aggregator.mapping)
    }

    Behaviors.receiveMessage[Command] {
      case Message(id, content) =>
        //fetches some metadata and
        router ! Aggregator.Enriched(id, "metadata")
        Behaviors.same
    }
  }
}

object Aggregator {

  val serviceKey = ServiceKey[Aggregator.Command]("agg-key")

  def mapping(command: Command) = command.id

  sealed trait Command {
    def id: String
  }
  case class Obfuscated(id: String, content: String) extends Command
  case class Enriched(id: String, metadata: String) extends Command

  sealed trait Event
  case class Completed(id: String, content: String, metadata: String)
      extends Event

  def apply(
      messages: Map[String, String] = Map(),
      forwardTo: ActorRef[Event]): Behavior[Command] =
    Behaviors.setup[Command] { context =>

      context.system.receptionist ! Receptionist.Register(
        serviceKey,
        context.self)

      Behaviors.receiveMessage {
        case v @ Obfuscated(id, content) => {
          messages.get(id) match {
            case Some(metadata) =>
              forwardTo ! Completed(id, content, metadata)
              apply(messages - (id, metadata), forwardTo)
            case None =>
              apply(messages + (id -> content), forwardTo)
          }
        }
        case e @ Enriched(id, metadata) => {
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
}
