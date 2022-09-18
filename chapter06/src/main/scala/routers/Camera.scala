package routers

import akka.actor.typed.scaladsl.{
  Behaviors,
  GroupRouter,
  PoolRouter,
  Routers
}
import akka.actor.typed.{ ActorRef, Behavior }
import akka.actor.typed.receptionist.{ Receptionist, ServiceKey }

object PhotoProcessor {
  val Key = ServiceKey[String]("photo-procesor-key")
  def apply(): Behavior[String] = Behaviors.ignore
}

object Camera {

  case class Photo(content: String)

  def apply() = Behaviors.setup[Photo] { context =>

    val routingBehavior: GroupRouter[String] =
      Routers.group(PhotoProcessor.Key)
    val router: ActorRef[String] =
      context.spawn(routingBehavior, "photo-processor-pool")

    Behaviors.receiveMessage {
      case Photo(content) =>
        router ! content
        Behaviors.same
    }
  }
}
