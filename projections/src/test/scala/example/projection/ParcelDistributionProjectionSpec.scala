package example.projection

import org.scalatest.wordspec.AnyWordSpecLike
import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit

import akka.projection.testkit.scaladsl.{
  ProjectionTestKit,
  TestProjection,
  TestSourceProvider
}

import akka.persistence.query.Offset

import akka.projection.eventsourced.EventEnvelope
import akka.projection.scaladsl.Handler
import akka.projection.ProjectionId

import akka.stream.scaladsl.Source

import scala.concurrent.{ ExecutionContext, Future }
import akka.Done

import example.persistence.ShippingShardingVehicle
import example.persistence.ShippingShardingVehicle._
import example.persistence.InMemParcelPerSizeRepository

class ParcelDistributionProjectionSpec
    extends ScalaTestWithActorTestKit
    with AnyWordSpecLike {

  /**
   *
   * wrap it on a future calling to its 'proccess' method
   * doesn't need session because its repo is our fake inMem
   * and maps it to a Handler[EventEnvelope[ShippingShardingVehicle.Event]]
      **/
  private def toAsyncHandler(
      handler: ParcelDistributionProjectionHandler)(
      implicit ec: ExecutionContext)
      : Handler[EventEnvelope[ShippingShardingVehicle.Event]] =
    eventEnvelope =>
      Future {
        handler.process(session = null, eventEnvelope)
        Done
      }
  "a projection" should {
    "be able to read from journal and store in mem storage" in {

      val vehicleId1 = "vh1"
      val vehicleId2 = "vh2"

      val tag = "vh1-tag" //TODO? what is this for?

      val myEvents =
        Source(
          List[EventEnvelope[ShippingShardingVehicle.Event]](
            new EventEnvelope(
              offset = Offset.sequence(0L),
              persistenceId = vehicleId1,
              sequenceNr = 10,
              event = ParcelAdded(vehicleId1, Parcel("p1", "MEDIUM")),
              timestamp = 0L),
            new EventEnvelope(
              offset = Offset.sequence(1L),
              persistenceId = vehicleId1,
              sequenceNr = 11,
              event = ParcelAdded(vehicleId1, Parcel("p2", "MEDIUM")),
              timestamp = 2L)))

      val inMemRepository = new InMemParcelPerSizeRepository()

      val projectionId = ProjectionId("per-size-1", vehicleId1)

      val sourceProvider = TestSourceProvider[
        Offset,
        EventEnvelope[ShippingShardingVehicle.Event]](
        sourceEvents = myEvents,
        extractOffset = env => env.offset)

      val projection =
        TestProjection[
          Offset,
          EventEnvelope[ShippingShardingVehicle.Event]](
          projectionId,
          sourceProvider,
          handler = () =>
            toAsyncHandler(
              new ParcelDistributionProjectionHandler(
                tag,
                system,
                inMemRepository))(system.executionContext))

      val projectionTestKit = ProjectionTestKit(system)

      projectionTestKit.run(projection) {
        inMemRepository.getCount(null, "MEDIUM") shouldBe Some(2)
      }

    }
  }
}
