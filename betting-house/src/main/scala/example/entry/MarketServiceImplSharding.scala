package example.market.grpc

import akka.actor.typed.{ ActorRef, ActorSystem }
import akka.cluster.sharding.typed.scaladsl.{
  ClusterSharding,
  Entity
}

import akka.util.Timeout
import java.time.{ Instant, OffsetDateTime, ZoneId }

import example.betting.Market

import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration._
import scala.util.{ Failure, Success, Try }

import akka.stream.scaladsl.Flow
import akka.NotUsed
import akka.stream.typed.scaladsl.ActorFlow

class MarketServiceImplSharding(implicit sharding: ClusterSharding)
    extends MarketService {

  implicit val timeout: Timeout = 3.seconds
  implicit val executionContext: ExecutionContext =
    ExecutionContext.global

  sharding.init(Entity(Market.typeKey)(entityContext =>
    Market(entityContext.entityId)))

  override def cancel(in: example.market.grpc.CancelMarket)
      : scala.concurrent.Future[example.market.grpc.Response] = {
    val market = sharding.entityRefFor(Market.typeKey, in.marketId)
    def auxCancel(reason: String)(
        replyTo: ActorRef[Market.Response]) =
      Market.Cancel(in.reason, replyTo)

    market
      .ask(auxCancel(in.reason))
      .mapTo[Market.Response]
      .map { response =>
        response match {
          case Market.Accepted =>
            example.market.grpc.Response("initialized")
          case Market.RequestUnaccepted(reason) =>
            example.market.grpc
              .Response(s"market NOT cancelled because [$reason]")
        }
      }
  }

  override def closeMarket(in: example.market.grpc.MarketId)
      : scala.concurrent.Future[example.market.grpc.Response] = {
    val market = sharding.entityRefFor(Market.typeKey, in.marketId)

    market
      .ask(Market.Close)
      .mapTo[Market.Response]
      .map { response =>
        response match {
          case Market.Accepted =>
            example.market.grpc.Response("initialized")
          case Market.RequestUnaccepted(reason) =>
            example.market.grpc
              .Response(s"market NOT closed because [$reason]")
        }
      }
  }
  override def getState(in: example.market.grpc.MarketId)
      : scala.concurrent.Future[example.market.grpc.MarketData] = {
    val market = sharding.entityRefFor(Market.typeKey, in.marketId)

    market.ask(Market.GetState).mapTo[Market.CurrentState].map {
      state =>
        val (
          marketId,
          Market.Fixture(id, homeTeam, awayTeam),
          Market.Odds(winHome, winAway, draw)) = (
          state.status.marketId,
          state.status.fixture,
          state.status.odds)

        MarketData(
          marketId,
          Some(FixtureData(id, homeTeam, awayTeam)),
          Some(OddsData(winHome, winAway, draw)))
    }

  }

  override def open(in: example.market.grpc.MarketData)
      : scala.concurrent.Future[example.market.grpc.Response] = {
    val market = sharding.entityRefFor(Market.typeKey, in.marketId)

    def auxInit(in: MarketData)(
        replyTo: ActorRef[Market.Response]) = {

      val fixture = in.fixture match {
        case Some(FixtureData(id, homeTeam, awayTeam, _)) =>
          Market.Fixture(id, homeTeam, awayTeam)
        case None =>
          throw new IllegalArgumentException(
            "Fixture is empty. Not allowed")
      }

      val odds = in.odds match {
        case Some(OddsData(winHome, winAway, tie, _)) =>
          Market.Odds(winHome, winAway, tie)
        case None =>
          throw new IllegalArgumentException(
            "Odds are empty. Not allowed")
      }

      val opensAt = OffsetDateTime
        .ofInstant(Instant.ofEpochMilli(in.opensAt), ZoneId.of("UTC"))

      Market.Open(fixture, odds, opensAt, replyTo)

    }

    market
      .ask(auxInit(in))
      .mapTo[Market.Response]
      .map { response =>
        response match {
          case Market.Accepted =>
            example.market.grpc.Response("initialized")
          case Market.RequestUnaccepted(reason) =>
            example.market.grpc
              .Response(s"market NOT initialized because [$reason]")
        }
      }
  }

  override def update(
      in: akka.stream.scaladsl.Source[
        example.market.grpc.MarketData,
        akka.NotUsed]): akka.stream.scaladsl.Source[
    example.market.grpc.Response,
    akka.NotUsed] = {

    def auxUpdate(marketData: MarketData)(
        replyTo: ActorRef[Market.Response]): Market.Update = {

      val odds = marketData.odds.map(m =>
        Market.Odds(m.winHome, m.winAway, m.tie))

      val opensAt =
        Some(
          OffsetDateTime
            .ofInstant(
              Instant.ofEpochMilli(marketData.opensAt),
              ZoneId.of("UTC")))

      val result = Some(marketData.result.value)

      Market.Update(odds, opensAt, result, replyTo)

    }

    in.mapAsync(10) { marketData =>
      val marketRef =
        sharding.entityRefFor(Market.typeKey, marketData.marketId)

      marketRef
        .ask(auxUpdate(marketData))
        .mapTo[Market.Response]
        .map { response =>
          response match {
            case Market.Accepted =>
              example.market.grpc.Response("Updated")
            case Market.RequestUnaccepted(reason) =>
              example.market.grpc
                .Response(s"market NOT updated because [$reason]")
          }
        }
    }

  }

}
