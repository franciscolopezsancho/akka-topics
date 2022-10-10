package betting.house.projection

import org.slf4j.LoggerFactory
import akka.projection.eventsourced.EventEnvelope
import akka.projection.jdbc.scaladsl.JdbcHandler
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
      case Bet.Opened(
          betId,
          walletId,
          marketId,
          odds,
          stake,
          result) =>
        repository.addBet(
          betId,
          walletId,
          marketId,
          odds,
          stake,
          result,
          session)
      case x =>
        logger.debug("ignoring event {} in projection", x)

    }
  }
}
