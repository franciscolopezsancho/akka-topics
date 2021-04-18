package example.projection

import scalikejdbc.{ DBSession, NamedAutoSession }

import example.persistence.ShippingShardingVehicle
import example.persistence.ParcelPerSizeRepository

import akka.actor.typed.ActorSystem

import akka.persistence.query.Offset

import akka.persistence.jdbc.query.scaladsl.JdbcReadJournal

import akka.projection.ProjectionId
import akka.projection.eventsourced.EventEnvelope
import akka.projection.scaladsl.{ ExactlyOnceProjection }
import akka.projection.eventsourced.scaladsl.{ EventSourcedProvider }
import akka.projection.jdbc.scaladsl.{ JdbcHandler, JdbcProjection }

object ParcelDistributionProjection {
  def createProjectionFor(
      system: ActorSystem[_],
      repository: ParcelPerSizeRepository,
      index: Int): ExactlyOnceProjection[
    Offset,
    EventEnvelope[ShippingShardingVehicle.Event]] = {

    val tag = ShippingShardingVehicle.tags(index)

    val sourceProvider =
      EventSourcedProvider.eventsByTag[ShippingShardingVehicle.Event](
        system = system,
        readJournalPluginId = JdbcReadJournal.Identifier,
        tag = tag)

    JdbcProjection.exactlyOnce(
      projectionId =
        ProjectionId("ParcelDistributionProjection", tag),
      sourceProvider = sourceProvider,
      handler = () =>
        new ParcelDistributionProjectionHandler(
          tag,
          system,
          repository),
      sessionFactory = () => new ScalikeJdbcSession())(system)
  }
}

class ParcelDistributionProjectionHandler(
    tag: String,
    system: ActorSystem[_],
    repository: ParcelPerSizeRepository)
    extends JdbcHandler[
      EventEnvelope[ShippingShardingVehicle.Event],
      ScalikeJdbcSession]() {

  override def process(
      session: ScalikeJdbcSession,
      envelope: EventEnvelope[ShippingShardingVehicle.Event]) =
    envelope.event match {
      case ShippingShardingVehicle.ParcelAdded(vehicleId, parcel) =>
        repository.update(session, parcel)
    }

}

import java.sql.Connection
import scalikejdbc.DB
import akka.projection.jdbc.JdbcSession
import akka.japi.function.Function

object ScalikeJdbcSession {
  def withSession[R](f: ScalikeJdbcSession => R): R = {
    val session = new ScalikeJdbcSession()
    try {
      f(session)
    } finally {
      session.close()
    }
  }
}

/**
 * Provide database connections within a transaction to Akka Projections.
 */
final class ScalikeJdbcSession extends JdbcSession {
  val db: DB = DB.connect()
  db.autoClose(false)

  override def withConnection[Result](
      func: Function[Connection, Result]): Result = {
    db.begin()
    db.withinTxWithConnection(func(_))
  }

  override def commit(): Unit = db.commit()

  override def rollback(): Unit = db.rollback()

  override def close(): Unit = db.close()
}
