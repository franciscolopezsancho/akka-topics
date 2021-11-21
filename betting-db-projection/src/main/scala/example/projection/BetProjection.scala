package example.projection

import org.slf4j.LoggerFactory

import akka.actor.typed.ActorSystem

import akka.projection.ProjectionId
import akka.projection.scaladsl.ExactlyOnceProjection
import akka.projection.eventsourced.EventEnvelope
import akka.projection.eventsourced.scaladsl.EventSourcedProvider

import akka.projection.jdbc.scaladsl.{ JdbcHandler, JdbcProjection }

import akka.persistence.query.Offset
import akka.persistence.jdbc.query.scaladsl.JdbcReadJournal

import example.repository.scalike.{
  BetRepository,
  ScalikeJdbcSession
}

import example.betting.Bet

//Bets grouped per Market and Wallet
object BetProjection { //BPM

  val logger =
    LoggerFactory.getLogger(BetProjection + "")

  def createProjectionFor(
      system: ActorSystem[_],
      repository: BetRepository,
      indexTag: Int)
      : ExactlyOnceProjection[Offset, EventEnvelope[Bet.Event]] = {

    val tag = "bet-tag-" + indexTag

    val sourceProvider =
      EventSourcedProvider.eventsByTag[Bet.Event](
        system = system,
        readJournalPluginId = JdbcReadJournal.Identifier,
        tag = tag)

    JdbcProjection.exactlyOnce(
      projectionId = ProjectionId("BetProjection", tag),
      sourceProvider = sourceProvider,
      handler = () => new ProjectionHandler(repository),
      sessionFactory = () => new ScalikeJdbcSession())(system)
  }
}

class ProjectionHandler(repository: BetRepository)
    extends JdbcHandler[EventEnvelope[Bet.Event], ScalikeJdbcSession] {

  val logger = LoggerFactory.getLogger(classOf[ProjectionHandler])

  override def process(
      session: ScalikeJdbcSession,
      envelope: EventEnvelope[Bet.Event]): Unit = {
    envelope.event match {
      case Bet.Opened(betId, walletId, marketId, _, stake, _) =>
        repository.addBet(betId, walletId, marketId, stake, session)
      case x =>
        logger.debug("ignoring event {} in projection", x)

    }
  }
}
