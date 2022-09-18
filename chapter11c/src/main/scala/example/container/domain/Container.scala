package example.container.domain

import akka.actor.typed.{ ActorRef, Behavior }
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.sharding.typed.scaladsl.EntityTypeKey

object Container {

  val TypeKey = EntityTypeKey[Command]("container")

  case class Cargo(kind: String, size: Int)
  case class Cargos(cargos: List[Cargo])

  sealed trait Command
  case class AddCargo(cargo: Cargo) extends Command
  case class GetCargos(replyTo: ActorRef[Cargos]) extends Command

  def apply(
      entityId: String,
      cargos: List[Cargo] = Nil): Behavior[Command] = {
    Behaviors.receive { (context, message) =>
      message match {
        case AddCargo(cargo) =>
          println(s"adding cargo $cargo")
          apply(entityId, cargo +: cargos)
        case GetCargos(replyTo) =>
          replyTo ! Cargos(cargos)
          Behaviors.same
      }
    }
  }
}
