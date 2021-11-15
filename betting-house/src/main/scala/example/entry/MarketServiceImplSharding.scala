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

class MarketServiceImplSharding(implicit sharding: ClusterSharding)
    extends MarketService {

  implicit val timeout: Timeout = 3.seconds
  implicit val executionContext: ExecutionContext =
    ExecutionContext.global

  val shardingRegion =
    sharding.init(Entity(Market.TypeKey)(entityContext =>
      Market(entityContext.entityId)))

  override def cancel(in: example.market.grpc.CancelMarket)
      : scala.concurrent.Future[example.market.grpc.Response] = {
    val market = sharding.entityRefFor(Market.TypeKey, in.marketId)
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
    val market = sharding.entityRefFor(Market.TypeKey, in.marketId)

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
    val market = sharding.entityRefFor(Market.TypeKey, in.marketId)

    market.ask(Market.GetState).mapTo[Market.CurrentState].map {
      state =>
        val (
          marketId,
          Market.Fixture(id, homeTeam, awayTeam),
          Market.Odds(winHome, winAway, tie)) = (
          state.status.marketId,
          state.status.fixture,
          state.status.odds)

        MarketData(
          marketId,
          Some(FixtureData(id, homeTeam, awayTeam)),
          Some(OddsData(winHome, winAway, tie)))
    }

  }
  override def initialize(in: example.market.grpc.MarketData)
      : scala.concurrent.Future[example.market.grpc.Response] = {
    val market = sharding.entityRefFor(Market.TypeKey, in.marketId)
    val fixture = in.fixture match {
      case Some(FixtureData(id, homeTeam, awayTeam, _)) =>
        Market.Fixture(id, homeTeam, awayTeam)
      //TODO case or for comprehensions
    }

    val odds = in.odds match {
      case Some(OddsData(winHome, winAway, tie, _)) =>
        Market.Odds(winHome, winAway, tie)
      //TODO case or for comprehensions
    }

    val opensAt = OffsetDateTime
      .ofInstant(Instant.ofEpochMilli(in.opensAt), ZoneId.of("UTC"))

    def initMessage(
        f: Market.Fixture,
        o: Market.Odds,
        opensAt: OffsetDateTime)(replyTo: ActorRef[Market.Response]) =
      Market.Initialize(f, o, opensAt, replyTo)

    market
      .ask(initMessage(fixture, odds, opensAt))
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
  override def open(in: example.market.grpc.MarketId)
      : scala.concurrent.Future[example.market.grpc.Response] = {
    val market = sharding.entityRefFor(Market.TypeKey, in.marketId)

    market
      .ask(Market.Open)
      .mapTo[Market.Response]
      .map { response =>
        response match {
          case Market.Accepted =>
            example.market.grpc.Response("initialized")
          case Market.RequestUnaccepted(reason) =>
            example.market.grpc
              .Response(s"market NOT opened because [$reason]")
        }
      }
  }
  override def update(
      in: akka.stream.scaladsl.Source[
        example.market.grpc.MarketData,
        akka.NotUsed]): akka.stream.scaladsl.Source[
    example.market.grpc.Response,
    akka.NotUsed] = {

    def auxAskFlow(
        marketData: MarketData,
        marketRef: ActorRef[Market.Command])
        : Flow[Int, Int, NotUsed] = ???

    // in.mapConcat { marketData =>
    //   val marketRef =
    //     sharding.entityRefFor(Market.TypeKey, marketData.marketId)
    //   (marketData, marketRef)
    // }
    ???
  }

}
