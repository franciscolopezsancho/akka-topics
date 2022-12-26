package routers

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.PoolRouter
import akka.actor.typed.scaladsl.Routers

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
