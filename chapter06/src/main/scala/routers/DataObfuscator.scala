package routers

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.Routers

object DataObfuscator {
  sealed trait Command

  final case class Message(id: String, content: String)
      extends Command

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
