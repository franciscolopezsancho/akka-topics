package routers

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.PoolRouter
import akka.actor.typed.scaladsl.Routers

object BroadcastingChecker {

  def apply(behavior: Behavior[HighWayPatrol.Command]) =
    Behaviors.setup[Unit] { context =>
      val poolSize = 4
      val dataCheckerRouter: PoolRouter[HighWayPatrol.Command] =
        Routers
          .pool(poolSize = 4)(behavior)
          .withBroadcastPredicate(msg =>
            msg.isInstanceOf[HighWayPatrol.Violation])
      ???
    }

}
