package example.container.domain

import akka.actor.typed.{ ActorRef, Behavior }
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.sharding.typed.scaladsl.EntityTypeKey

object Container {

  val TypeKey = EntityTypeKey[Command]("container")

  final case class Cargo(kind: String, size: Int)
  final case class Cargos(cargos: List[Cargo])

  sealed trait Command
  final case class AddCargo(cargo: Cargo) extends Command
  final case class GetCargos(replyTo: ActorRef[Cargos])
      extends Command

  def apply(
      entityId: String,
      cargos: List[Cargo] = Nil): Behavior[Command] = {
    Behaviors.receive { (context, message) =>
      message match {
        case AddCargo(cargo) =>
          context.log.info(s"adding cargo $cargo")
          apply(entityId, cargo +: cargos)
        case GetCargos(replyTo) =>
          replyTo ! Cargos(cargos)
          Behaviors.same
      }
    }
  }
}
