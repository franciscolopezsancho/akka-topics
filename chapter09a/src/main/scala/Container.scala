package example.sharding


import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.sharding.typed.scaladsl.EntityTypeKey

object Container {

  val TypeKey =
    EntityTypeKey[Container.Command]("container-type-key")

  final case class Cargo(id: String, kind: String, size: Int)

  sealed trait Command

  final case class AddCargo(cargo: Cargo)
    extends Command
      with CborSerializable

  final case class GetCargos(replyTo: ActorRef[List[Cargo]])
    extends Command
      with CborSerializable

  def apply(containerId: String): Behavior[Command] = {
    ready(List())
  }

  def ready(cargos: List[Cargo]): Behavior[Command] = {
    Behaviors.receiveMessage[Command] {
      case AddCargo(cargo) =>
        ready(cargo +: cargos)
      case GetCargos(replyTo) =>
        replyTo ! cargos
        Behaviors.same
    }
  }
}
