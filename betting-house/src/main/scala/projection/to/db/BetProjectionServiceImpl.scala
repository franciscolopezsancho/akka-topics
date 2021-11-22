package betting.house.projection.proto

import akka.actor.typed.{ ActorSystem, DispatcherSelector }
import scala.concurrent.{ ExecutionContext, Future }

import example.repository.scalike.{
  BetRepository,
  ScalikeJdbcSession
}

class BetProjectionServiceImpl(
    system: ActorSystem[_],
    betRepository: BetRepository)
    extends BetProjectionService {

  implicit private val jdbcExecutor: ExecutionContext =
    system.dispatchers.lookup(
      DispatcherSelector.fromConfig(
        "akka.projection.jdbc.blocking-jdbc-dispatcher"))

  def getBetByMarket(
      in: MarketIdsBet): scala.concurrent.Future[SumStake] = {
    Future {
      ScalikeJdbcSession.withSession { session =>
        val result: Option[Long] = betRepository
          .getBetPerMarketTotalStake(in.marketId, session)
        SumStake(result.getOrElse(0))
      }
    }
  }
}
