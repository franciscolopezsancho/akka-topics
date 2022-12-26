package routers

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.PoolRouter
import akka.actor.typed.scaladsl.Routers

object PostalOfficeManager {

  def apply(behavior: Behavior[PostalOffice.Command]) =
    Behaviors.setup[Unit] { context =>

      val routingBehavior: PoolRouter[PostalOffice.Command] =
        Routers
          .pool(poolSize = 4)(behavior)
          .withBroadcastPredicate(msg =>
            msg.isInstanceOf[PostalOffice.Guaranteed])

      val router: ActorRef[PostalOffice.Command] =
        context.spawn(routingBehavior, "test-pool")

      (0 to 10).foreach { n =>
        router ! PostalOffice.Guaranteed("payslip")
      }
      Behaviors.empty
    }
}
