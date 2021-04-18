package example.persistence

import com.typesafe.config.ConfigFactory

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorRef, Behavior }

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

//TODO to have one to show sharding and another for projection
object ShippingShardingVehicle {

  val TypeKey =
    EntityTypeKey[ShippingShardingVehicle.Command]("vehicle-type-key")

  case class Parcel(id: String, size: String)
  sealed trait Command
  case class AddParcel(parcel: Parcel) extends Command
  case class GetParcels(replyTo: ActorRef[List[Parcel]])
      extends Command

  sealed trait Event
  case class ParcelAdded(vehicleId: String, parcel: Parcel)
      extends Event

  final case class State(parcels: List[Parcel] = Nil)

  val tags = Vector.tabulate(3)(i => s"vehicle-$i")

  def apply(vehicleId: String): Behavior[Command] =
    EventSourcedBehavior[Command, Event, State](
      PersistenceId(TypeKey.name, vehicleId),
      State(),
      commandHandler =
        (state, command) => commandHandler(vehicleId, state, command),
      eventHandler)

  def commandHandler(
      vehicleId: String,
      state: State,
      command: Command): Effect[Event, State] =
    command match {
      case AddParcel(parcel) =>
        Effect.persist(ParcelAdded(vehicleId, parcel))
      case GetParcels(replyTo) =>
        Effect.none.thenRun(state => replyTo ! state.parcels)
    }

  def eventHandler(state: State, event: Event): State =
    event match {
      case ParcelAdded(vehicleId, parcel) =>
        state.copy(parcels = parcel +: state.parcels)
    }
}

object ShippingVehicle {

  sealed trait Command
  case class AddParcel(id: String) extends Command
  case class GetParcels(replyTo: ActorRef[List[String]])
      extends Command

  sealed trait Event
  case class ParcelAdded(parcelId: String) extends Event

  final case class State(parcels: List[String] = Nil)

  def apply(vehiclePersistenceId: String): Behavior[Command] =
    EventSourcedBehavior[Command, Event, State](
      PersistenceId.ofUniqueId(vehiclePersistenceId),
      State(),
      commandHandler,
      eventHandler)

  def commandHandler(
      state: State,
      command: Command): Effect[Event, State] =
    command match {
      case AddParcel(id) => Effect.persist(ParcelAdded(id))
      case GetParcels(replyTo) =>
        Effect.none.thenRun(state => replyTo ! state.parcels)
    }

  def eventHandler(state: State, event: Event): State =
    event match {
      case ParcelAdded(id) =>
        state.copy(parcels = id +: state.parcels)
    }
}
