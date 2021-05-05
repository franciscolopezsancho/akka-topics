package example.projection

import scalikejdbc.{ DBSession, NamedAutoSession }

import org.slf4j.LoggerFactory

import example.persistence.Parcel
import example.persistence.VisitedCitiesRepository
import example.persistence.ScalikeJdbcSession

import akka.actor.typed.ActorSystem

import akka.persistence.query.Offset

import akka.persistence.jdbc.query.scaladsl.JdbcReadJournal

import akka.projection.ProjectionId
import akka.projection.eventsourced.EventEnvelope
import akka.projection.scaladsl.{ ExactlyOnceProjection }
import akka.projection.eventsourced.scaladsl.{ EventSourcedProvider }
import akka.projection.jdbc.scaladsl.{ JdbcHandler, JdbcProjection }

object VisitedCitiesProjection {

  val logger = LoggerFactory.getLogger(VisitedCitiesProjection+"")

  def createProjectionFor(
      system: ActorSystem[_],
      repository: VisitedCitiesRepository,
      index: Int)
      : ExactlyOnceProjection[Offset, EventEnvelope[Parcel.Event]] = {

    logger.debug("creating projection")

    val tag = Parcel.tags(index)
    val sourceProvider =
      EventSourcedProvider.eventsByTag[Parcel.Event](
        system = system,
        readJournalPluginId = JdbcReadJournal.Identifier,
        tag = tag)

    JdbcProjection.exactlyOnce(
      projectionId = ProjectionId("VisitedCitiesProjection", tag),
      sourceProvider = sourceProvider,
      handler = () =>
        new VisitedCitiesProjectionHandler(tag, system, repository),
      sessionFactory = () => new ScalikeJdbcSession())(system)
  }
}

class VisitedCitiesProjectionHandler(
    tag: String,
    system: ActorSystem[_],
    repository: VisitedCitiesRepository)
    extends JdbcHandler[
      EventEnvelope[Parcel.Event],
      ScalikeJdbcSession]() {

  val logger =
    LoggerFactory.getLogger(classOf[VisitedCitiesProjectionHandler])

  override def process(
      session: ScalikeJdbcSession,
      envelope: EventEnvelope[Parcel.Event]) =
    envelope.event match {
      case Parcel.HandedOver(parcelId, location, time) =>
        logger.debug(s"processing HandOver parcelId '$parcelId'")
        repository.addCity(session, location.city)
      case Parcel.DestinationAdded(_, _, _) =>
        logger.debug(
          "destination added, not included in the projection")
    }

}
