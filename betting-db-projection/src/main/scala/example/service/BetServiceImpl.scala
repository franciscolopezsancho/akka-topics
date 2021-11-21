package example.bet.grpc

import akka.actor.typed.{ ActorSystem, DispatcherSelector }
import scala.concurrent.{ ExecutionContext, Future }

import example.repository.scalike.{
  BetRepository,
  ScalikeJdbcSession
}

class BetServiceImpl(
    system: ActorSystem[_],
    betRepository: BetRepository)
    extends BetService {

  implicit private val jdbcExecutor: ExecutionContext =
    system.dispatchers.lookup(
      DispatcherSelector.fromConfig(
        "akka.projection.jdbc-dispatcher"))

  def getBetByMarket(in: example.bet.grpc.MarketId)
      : scala.concurrent.Future[example.bet.grpc.SumStake] = {
    Future {
      ScalikeJdbcSession.withSession { session =>
        val result: Option[Long] = betRepository
          .getBetPerMarketTotalStake(in.marketId, session)
        SumStake(result.getOrElse(0))
      }
    }
  }
}
