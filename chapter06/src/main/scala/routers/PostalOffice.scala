package routers

import akka.actor.typed.scaladsl.{ Behaviors, PoolRouter, Routers }
import akka.actor.typed.{ ActorRef, Behavior }

object PostalOffice {
  sealed trait Command
  final case class Standard(msg: String) extends Command
  final case class Tracked(msg: String) extends Command
  final case class Guaranteed(msg: String) extends Command

  def apply(): Behavior[Command] =
    Behaviors.empty
}

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
