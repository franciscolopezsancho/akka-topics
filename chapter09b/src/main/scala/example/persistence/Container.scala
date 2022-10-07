package example.persistence

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.Effect
import akka.persistence.typed.scaladsl.EventSourcedBehavior

//persistence container
object Container {

  final case class Cargo(id: String, kind: String, size: Int)

  sealed trait Command

  final case class AddCargo(cargo: Cargo) extends Command

  final case class GetCargos(replyTo: ActorRef[List[Cargo]])
    extends Command

  sealed trait Event

  final case class CargoAdded(containerId: String, cargo: Cargo)
    extends Event

  final case class State(cargos: List[Cargo] = Nil)

  def apply(containerId: String): Behavior[Command] =
    EventSourcedBehavior[Command, Event, State](
      PersistenceId.ofUniqueId(containerId),
      State(),
      commandHandler = (state, command) =>
        commandHandler(containerId, state, command),
      eventHandler)

  def commandHandler(
                      containerId: String,
                      state: State,
                      command: Command): Effect[Event, State] =
    command match {
      case AddCargo(cargo) =>
        Effect.persist(CargoAdded(containerId, cargo))
      case GetCargos(replyTo) =>
        Effect.none.thenRun(state => replyTo ! state.cargos)
    }

  def eventHandler(state: State, event: Event): State =
    event match {
      case CargoAdded(containerId, cargo) =>
        state.copy(cargos = cargo +: state.cargos)
    }
}
