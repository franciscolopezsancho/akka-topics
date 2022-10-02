package routers

import akka.actor.typed.scaladsl.{ Behaviors, GroupRouter, Routers }
import akka.actor.typed.ActorRef

object Camera {

  final case class Photo(content: String)

  def apply() = Behaviors.setup[Photo] { context =>

    val routingBehavior: GroupRouter[String] =
      Routers.group(PhotoProcessor.key).withRoundRobinRouting()
    val router: ActorRef[String] =
      context.spawn(routingBehavior, "photo-processor-pool")

    Behaviors.receiveMessage {
      case Photo(content) =>
        router ! content
        Behaviors.same
    }
  }
}
