import scalikejdbc.DBSession

// trait ParcelDistributionRepository {
//   def update(
//       session: DBSession,
//       parcelId: String,
//       quantity: Int): Unit
//   def getItem(session: DBSession, parcelId: String): Option[String]
// }

// class InMemParcelDistributionRepository
//     extends ParcelDistributionRepository {
//   var parcels: Map[String, Int] = Map.empty
//   override def update(
//       session: DBSession,
//       parcelId: String,
//       quantity: Int) = {
//     parcels =
//       parcels + (parcelId -> parcels.getOrElse(parcelId, 0) + quantity)
//   }

//   override def getItem(
//       session: DBSession,
//       parcelId: String): Option[String] = parcels.get(parcelId)

// }

// import akka.actor.typed.ActorSystem

// import akka.projection.eventsourced.EventEnvelope
// import akka.projection.jdbc.scala.JdbcHandler

// import example.persistence.ShippingShardingVehicle

// class ParcelDistributionProjectionHandler(
//     tag: String,
//     system: ActorSystem[_],
//     repository: ParcelDistributionRepository)
//     extends JdbcHandler[
//       EventEnvelope[ShippingShardingVehicle.Event, DBSession]] {

//   override def process(
//       session: DBSession,
//       envelope: EventEnvelope[ShippingShardingVehicle.Event]) =
//     envelope.event match {
//       case AddParcel(parcelId) =>
//         repository.update(parcelId, 0)
//     }

// }

// import akka.projection.jdbc.scaladsl.JdbcProjection

// object ParcelDistributionProjection {
//   def createProjectionFor(
//       system: ActorSystem[_],
//       repository: ParcelDistributionRepository,
//       Index: Int): ExactlyOnceProjection[
//     Offset,
//     EventEnvelope[ShippingShardingVehicle]] = {
//     val sourceProvider =
//       EventSourceProvider.eventsByTag[ShippingShardingVehicle.Event](
//         system = system,
//         readJournalPluginId = JdbcReadJournal.Identifier tag = tag)
//     JdbcProjection.exactlyOnce(
//       projectionId =
//         ProjectionId("ParcelDistributionProjection", tag),
//       sourceProvider,
//       handler = () =>
//         new ParcelDistributionProjectionHandler(
//           tag,
//           system,
//           repository),
//       sessionFactory = () => new DBSession())
//     (system)
//   }
// }
