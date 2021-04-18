package example.projection

import org.scalatest.wordspec.AnyWordSpecLike
import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit

import akka.persistence.projection.eventsourced.EventEnvelope

class ParcelDistributionProjectionSpec
    extends ScalaTestWithActorTestKit
    with AnyWordSpecLike {

  "a projection" should {
    "be able to read from journal and store in mem storage" in {

      val vehicleId1 = "vh1"
      val vehicleId2 = "vh2"

      val events =
        Source(
          List[EventEnvelope[ShoppingShardingVehicle.Event]](
            new EventEnvelope(
              offset = Offset.sequence(0L),
              persistenceId = vehicleId1,
              sequenceNr = 10,
              event =
                ParcelAdded(vehicleId1, Parcel("p1", "MEDIUM")) timestamp =
                  0L)))

    }
  }
}
