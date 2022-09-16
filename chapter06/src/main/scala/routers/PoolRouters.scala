package routers

import akka.actor.typed.scaladsl.{
  Behaviors,
  PoolRouter,
  Routers
}
import akka.actor.typed.{ ActorRef, Behavior }

object Worker {
  def apply(monitor: ActorRef[String]): Behavior[String] =
    Behaviors.receiveMessage[String] {
      case message =>
        monitor ! message
        Behaviors.same
    }
}

object Manager {
  def apply(behavior: Behavior[String]) = Behaviors.setup[Unit] {
    context =>
      val routingBehavior: Behavior[String] =
        Routers.pool(poolSize = 4)(behavior)
      val router: ActorRef[String] =
        context.spawn(routingBehavior, "test-pool")
      (0 to 10).foreach { n =>
        router ! "hi"
      }
      Behaviors.empty
  }
}

object BroadcastingManager {
  def apply(behavior: Behavior[String]) = Behaviors.setup[Unit] {
    context =>
      val poolSize = 4

      val routingBehavior: PoolRouter[String] =
        Routers
          .pool(poolSize = poolSize)(behavior)
          .withBroadcastPredicate(msg => msg.length > 5)

      val router: ActorRef[String] =
        context.spawn(routingBehavior, "test-pool")

      (0 to 10).foreach { n =>
        router ! "hi, there"
      }

      Behaviors.empty
  }
}