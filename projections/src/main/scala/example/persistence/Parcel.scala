package example.persistence

import java.time.Instant

import scala.concurrent.duration._

import com.typesafe.config.ConfigFactory

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorRef, Behavior, SupervisorStrategy }

import akka.persistence.typed.scaladsl.{
  Effect,
  EventSourcedBehavior,
  RetentionCriteria
}
import akka.persistence.typed.PersistenceId

import akka.cluster.sharding.typed.scaladsl.{
  ClusterSharding,
  Entity,
  EntityContext,
  EntityRef,
  EntityTypeKey
}
import akka.cluster.sharding.typed.ShardingEnvelope

//TODO to have one to show sharding and another for projection
object Parcel {

  val TypeKey =
    EntityTypeKey[Parcel.Command]("parcel-type-key")

  case class Location(
      city: String,
      address: String,
      postalCode: String)

  sealed trait Command
  case class AddDestination(location: Location, isFinal: Boolean)
      extends Command
  case class HandOver(location: Location, time: Instant)
      extends Command
  case class GetState(replyTo: ActorRef[Parcel.State]) extends Command

  sealed trait Event
  case class DestinationAdded(
      parcelId: String,
      location: Location,
      isFinal: Boolean)
      extends Event
      with CborSerializable
  case class HandedOver(
      parcelId: String,
      location: Location,
      time: Instant)
      extends Event
      with CborSerializable

  final case class State(
      destinations: Map[Location, Boolean] = Map.empty,
      location: Option[Location] = None)

  val tags = Vector.tabulate(3)(i => s"parcels-$i")

  def calculateTag(
      entityContext: EntityContext[Command],
      tags: Vector[String] = tags): String = {
    val tagIndex =
      math.abs(entityContext.entityId.hashCode % tags.size)
    tags(tagIndex)
  }

  def apply(
      parcelId: String,
      projectionTag: String): Behavior[Command] =
    EventSourcedBehavior[Command, Event, State](
      PersistenceId(TypeKey.name, parcelId),
      State(),
      commandHandler =
        (state, command) => commandHandler(parcelId, state, command),
      eventHandler)
      .withTagger { event =>
        event match {
          case HandedOver(_, _, _) =>
            Set(projectionTag, "parcel-hand-over")
          case _ => Set(projectionTag)
        }
      }

  def commandHandler(
      parcelId: String,
      state: State,
      command: Command): Effect[Event, State] =
    command match {
      case AddDestination(location, isFinal) =>
        Effect.persist(DestinationAdded(parcelId, location, isFinal))
      case HandOver(location, time) =>
        Effect.persist(HandedOver(parcelId, location, time))
      case GetState(replyTo) =>
        Effect.none.thenRun(state => replyTo ! state)
    }

  def eventHandler(state: State, event: Event): State =
    event match {
      case DestinationAdded(parcelId, location, isFinal) =>
        //TODO not worry now about multiple final destination
        state.copy(destinations =
          state.destinations + (location -> isFinal))
      case HandedOver(parcelId, location, time) => {
        //TODO not worry now about location not found in destinations
        state.copy(destinations = state.destinations - location)
      }

    }
}
