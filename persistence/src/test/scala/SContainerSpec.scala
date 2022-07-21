package example.persistence

import akka.actor.testkit.typed.scaladsl.{
  LogCapturing,
  ScalaTestWithActorTestKit
}

import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.matchers.should.Matchers

import akka.persistence.testkit.scaladsl.EventSourcedBehaviorTestKit

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

class SPContainerSpec
    extends ScalaTestWithActorTestKit(
      EventSourcedBehaviorTestKit.config.withFallback(
        ConfigFactory.load("application-test")))
    with AnyWordSpecLike
    with Matchers
    with LogCapturing {

  "a persistent entity with sharding" should {

    "be able to add container" in {
      val sharding = ClusterSharding(system)

      val shardRegion
          : ActorRef[ShardingEnvelope[SPContainer.Command]] =
        sharding.init(
          Entity(SPContainer.TypeKey)(createBehavior = entityContext =>
            SPContainer(entityContext.entityId)))

      val containerId = "id-1"
      val cargo = SPContainer.Cargo("id-c", "sack", 3)

      shardRegion ! ShardingEnvelope(
        containerId,
        SPContainer.AddCargo(cargo))

      val probe =
        createTestProbe[List[SPContainer.Cargo]]()
      val container: EntityRef[SPContainer.Command] =
        sharding.entityRefFor(SPContainer.TypeKey, containerId)
      container ! SPContainer.GetCargos(probe.ref)

      probe.expectMessage(List(cargo))

    }

  }
  // "a persistent entity with sharing and state" should {
  //   "be able to add parcels even when cleaning by stashing them" in {
  //     val sharding = ClusterSharding(system)

  //     val shardRegion: ActorRef[
  //       ShardingEnvelope[SPContainerFSM.Command]] =
  //       sharding.init(
  //         Entity(SPContainerFSM.TypeKey)(createBehavior =
  //           entityContext =>
  //             SPContainerFSM(entityContext.entityId)))

  //     val containerId = "id-2"
  //     val parcelCode = "ABC"

  //     shardRegion ! ShardingEnvelope(
  //       containerId,
  //       SPContainerFSM.Clean)

  //     shardRegion ! ShardingEnvelope(
  //       containerId,
  //       SPContainerFSM.AddParcel(parcelCode))

  //     shardRegion ! ShardingEnvelope(
  //       containerId,
  //       SPContainerFSM.AddParcel(parcelCode))

  //     val probe = createTestProbe[List[String]]()
  //     val truck: EntityRef[SPContainerFSM.Command] =
  //       sharding.entityRefFor(
  //         SPContainerFSM.TypeKey,
  //         containerId)
  //     truck ! SPContainerFSM.GetParcels(probe.ref)

  //     probe.expectMessage(List("no parcels, we're cleaning"))

  //   }
  // }
}
// object SPContainerFSM {

//   val TypeKey =
//     EntityTypeKey[SPContainerFSM.Command](
//       "vehicle-type-key")

//   sealed trait Command
//   case object Clean extends Command
//   case class AddParcel(id: String) extends Command
//   case class GetParcels(replyTo: ActorRef[List[String]])
//       extends Command

//   sealed trait Event
//   case class ParcelAdded(id: String) extends Event
//   case object CleanedRequested extends Event

//   sealed trait State {
//     def parcels: List[String]
//   }

//   final case class Ready(parcels: List[String]) extends State
//   final case class Cleaning(parcels: List[String] = Nil) extends State

//   def apply(containerId: String): Behavior[Command] =
//     EventSourcedBehavior[Command, Event, State](
//       PersistenceId(TypeKey.name, containerId),
//       Ready(List()),
//       commandHandler,
//       eventHandler)

//   def commandHandler(
//       state: State,
//       command: Command): Effect[Event, State] =
//     state match {
//       case c: Cleaning =>
//         command match {
//           case AddParcel(id) => Effect.stash
//           case GetParcels(replyTo) =>
//             Effect.none.thenRun(state =>
//               replyTo ! List("no parcels, we're cleaning"))
//           case Clean => Effect.none
//         }
//       case r: Ready =>
//         command match {
//           case AddParcel(id) =>
//             //call the drone to add the parcel
//             //is this blocking. Again there is many situation we could model
//             // like the vehicle being full.
//             Effect.persist(ParcelAdded(id))
//           case GetParcels(replyTo) =>
//             Effect.none.thenRun(state => replyTo ! state.parcels)
//           case Clean => Effect.persist(Cleaned)
//         }
//     }

//   def eventHandler(state: State, event: Event): State =
//     state match {
//       case c: Cleaning => state
//       case r: Ready =>
//         event match {
//           case ParcelAdded(id) =>
//             r.copy(parcels = id +: state.parcels)
//           case Cleaned => Ready(List())
//         }
//     }

//   def findAndSet(durationMillis: Int): Unit = {
//     val until = System.currentTimeMillis() + durationMillis
//     while (System.currentTimeMillis() < until) //cleaning
//       println("found")
//   }
// }
