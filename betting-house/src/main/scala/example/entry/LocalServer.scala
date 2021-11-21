package example.betting

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.management.scaladsl.AkkaManagement
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import scala.concurrent.{ ExecutionContext, Future }
import akka.http.scaladsl.{ Http, HttpConnectionContext }
import akka.http.scaladsl.model.{ HttpRequest, HttpResponse }
import com.typesafe.config.ConfigFactory

import example.market.grpc.{
  MarketServiceHandler,
  MarketServiceImplSharding
}

import example.bet.grpc.{ BetServiceHandler, BetServiceImplSharding }

import scala.io.StdIn

object LocalServer extends App {

  implicit val system =
    ActorSystem[Nothing](
      Behaviors.empty,
      "betting-house",
      ConfigFactory.load("local"))

  implicit val sharding = ClusterSharding(system)

  implicit val ec: ExecutionContext = system.executionContext

  val marketService: HttpRequest => Future[HttpResponse] =
    MarketServiceHandler.withServerReflection(
      new MarketServiceImplSharding())

  val betService: HttpRequest => Future[HttpResponse] =
    BetServiceHandler.withServerReflection(
      new BetServiceImplSharding())

  val bindingFutureMarket: Future[Http.ServerBinding] =
    Http().newServerAt("0.0.0.0", 9000).bind(marketService)

  val bindingFutureBet: Future[Http.ServerBinding] =
    Http().newServerAt("0.0.0.0", 9001).bind(betService)

  val bindingFutureWallet =
    Http()
      .newServerAt("0.0.0.0", 9002)
      .bind(new WalletService().route)

  println(
    s"servers at localhost:9000, localhost:9001, and localhost:9002 \nPress RETURN to stop")
  StdIn.readLine()
  val res = for {
    s1 <- bindingFutureMarket
    u1 <- s1.unbind
    s2 <- bindingFutureBet
    u2 <- s2.unbind
    s3 <- bindingFutureWallet
    u3 <- s3.unbind
  } yield (u1, u2, u3)

  res.onComplete(_ => system.terminate())
}
