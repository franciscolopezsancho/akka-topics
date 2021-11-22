package betting.house.projection

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

class BetProjectionHandler(repository: BetRepository)
    extends JdbcHandler[EventEnvelope[Bet.Event], ScalikeJdbcSession] {

  val logger = LoggerFactory.getLogger(classOf[BetProjectionHandler])

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
