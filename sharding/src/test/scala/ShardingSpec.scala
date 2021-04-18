import akka.actor.testkit.typed.scaladsl.{
  LogCapturing,
  ScalaTestWithActorTestKit
}

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorRef, Behavior }

import akka.cluster.sharding.typed.scaladsl.{
  ClusterSharding,
  Entity,
  EntityRef,
  EntityTypeKey
}
import akka.cluster.sharding.typed.ShardingEnvelope

import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.matchers.should.Matchers

class ShardingSpec
    extends ScalaTestWithActorTestKit
    with AnyWordSpecLike
    with Matchers
    with LogCapturing {

  "a sharded shipping entity" should {
    "be able to add a parcel" in {

      val sharding = ClusterSharding(system)

      val shardRegion: ActorRef[ShardingEnvelope[Vehicle.Command]] =
        sharding.init(
          Entity(Vehicle.TypeKey)(createBehavior = entityContext =>
            Vehicle(entityContext.entityId)))

      val vehicleId = "id-1"
      val parcelCode = "ABC"

      shardRegion ! ShardingEnvelope(
        vehicleId,
        Vehicle.AddParcel(parcelCode))

      val probe = createTestProbe[List[String]]()

      val truck: EntityRef[Vehicle.Command] =
        sharding.entityRefFor(Vehicle.TypeKey, vehicleId)

      truck ! Vehicle.GetParcels(probe.ref)
      probe.expectMessage(List(parcelCode))

    }
  }
}

object Vehicle {

  val TypeKey =
    EntityTypeKey[Vehicle.Command]("vehicle-type-key")

  sealed trait Command
  case class AddParcel(id: String) extends Command
  case class GetParcels(replyTo: ActorRef[List[String]])
      extends Command

  def apply(vehicleId: String): Behavior[Command] = {
    ready(List())
  }

  def ready(parcels: List[String]): Behavior[Command] = { //def has to be in because the vehicleId is necesary
    Behaviors.receiveMessage[Command] {
      case AddParcel(id) =>
        ready(id +: parcels)
      case GetParcels(replyTo) =>
        replyTo ! parcels
        Behaviors.same
    }
  }

}
