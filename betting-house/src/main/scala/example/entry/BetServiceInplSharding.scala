package example.bet.grpc

import akka.actor.typed.{ ActorRef, ActorSystem }
import akka.cluster.sharding.typed.scaladsl.{
  ClusterSharding,
  Entity
}

import akka.util.Timeout
import java.time.{ Instant, OffsetDateTime, ZoneId }

import example.betting.Bet

import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration._
import scala.util.{ Failure, Success, Try }

class BetServiceImplSharding(implicit sharding: ClusterSharding)
    extends BetService {

  implicit val timeout: Timeout = 3.seconds
  implicit val executionContext: ExecutionContext =
    ExecutionContext.global

  sharding.init(
    Entity(Bet.TypeKey)(entityContext => Bet(entityContext.entityId)))

  def cancel(in: example.bet.grpc.CancelMessage)
      : scala.concurrent.Future[example.bet.grpc.BetResponse] = {
    val bet = sharding.entityRefFor(Bet.TypeKey, in.betId)

    def auxCancel(reason: String)(replyTo: ActorRef[Bet.Response]) =
      Bet.Cancel(reason, replyTo)

    bet.ask(auxCancel(in.reason)).mapTo[Bet.Response].map {
      response =>
        response match {
          case Bet.Accepted =>
            example.bet.grpc.BetResponse("initialized")
          case Bet.RequestUnaccepted(reason) =>
            example.bet.grpc
              .BetResponse(s"Bet NOT cancelled because [$reason]")
        }
    }
  }
  def open(in: example.bet.grpc.Bet)
      : scala.concurrent.Future[example.bet.grpc.BetResponse] = {
    val bet = sharding.entityRefFor(Bet.TypeKey, in.betId)

    def auxOpen(
        walletId: String,
        marketId: String,
        odds: Double,
        stake: Int,
        result: Int)(replyTo: ActorRef[Bet.Response]) =
      Bet.Open(walletId, marketId, odds, stake, result, replyTo)

    bet
      .ask(
        auxOpen(
          in.walletId,
          in.marketId,
          in.odds,
          in.stake,
          in.result))
      .mapTo[Bet.Response]
      .map { response =>
        response match {
          case Bet.Accepted =>
            example.bet.grpc.BetResponse("initialized")
          case Bet.RequestUnaccepted(reason) =>
            example.bet.grpc
              .BetResponse(s"Bet NOT opened because [$reason]")
        }
      }
  }

  def settle(in: example.bet.grpc.SettleMessage)
      : scala.concurrent.Future[example.bet.grpc.BetResponse] = {
    val bet = sharding.entityRefFor(Bet.TypeKey, in.betId)

    def auxSettle(result: Int)(replyTo: ActorRef[Bet.Response]) =
      Bet.Settle(result, replyTo)

    bet.ask(auxSettle(in.result)).mapTo[Bet.Response].map {
      response =>
        response match {
          case Bet.Accepted =>
            example.bet.grpc.BetResponse("initialized")
          case Bet.RequestUnaccepted(reason) =>
            example.bet.grpc
              .BetResponse(s"Bet NOT settled because [$reason]")
        }
    }
  }

}
