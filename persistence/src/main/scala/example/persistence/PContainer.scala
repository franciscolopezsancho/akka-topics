package example.persistence

import com.typesafe.config.ConfigFactory

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorRef, Behavior, SupervisorStrategy }

import akka.persistence.typed.scaladsl.{
  Effect,
  EventSourcedBehavior
}
import akka.persistence.typed.PersistenceId

import akka.cluster.sharding.typed.scaladsl.{
  ClusterSharding,
  Entity,
  EntityRef,
  EntityTypeKey
}
import akka.cluster.sharding.typed.ShardingEnvelope

//take out if not adding taggers, retention and onPersistFailure
import akka.persistence.typed.scaladsl.RetentionCriteria
import scala.concurrent.duration._

object PContainer {

  val TypeKey =
    EntityTypeKey[PContainer.Command]("pcontainer-type-key")

  case class Cargo(id: String, kind: String, size: Int)

  sealed trait Command
  case class AddCargo(cargo: Cargo)
      extends Command
      with CborSerializable
  case class GetCargos(replyTo: ActorRef[List[Cargo]])
      extends Command
      with CborSerializable

  sealed trait Event
  case class CargoAdded(containerId: String, cargo: Cargo)
      extends Event
      with CborSerializable

  final case class State(cargos: List[Cargo] = Nil)

  def apply(containerId: String): Behavior[Command] =
    EventSourcedBehavior[Command, Event, State](
      PersistenceId(TypeKey.name, containerId),
      State(),
      commandHandler = (state, command) =>
        commandHandler(containerId, state, command),
      eventHandler)
      .withTagger {
        case _ => Set(calculateTag(containerId))
      }
      .withRetention(RetentionCriteria
        .snapshotEvery(numberOfEvents = 100, keepNSnapshots = 2))
      .onPersistFailure(
        SupervisorStrategy.restartWithBackoff(
          minBackoff = 10.seconds,
          maxBackoff = 60.seconds,
          randomFactor = 0.1))

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

  //TODO read from properties
  val tags = Vector.tabulate(3)(i => s"container-tag-$i")

  def calculateTag(
      entityId: String,
      tags: Vector[String] = tags): String = {
    val tagIndex =
      math.abs(entityId.hashCode % tags.size)
    tags(tagIndex)
  }

}
