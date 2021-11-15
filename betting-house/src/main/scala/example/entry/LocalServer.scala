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

import scala.io.StdIn

object LocalServer extends App {

  implicit val system =
    ActorSystem[Nothing](
      Behaviors.empty,
      "betting-house",
      ConfigFactory.load("local"))

  implicit val sharding = ClusterSharding(system)

  implicit val ec: ExecutionContext = system.executionContext

  val service: HttpRequest => Future[HttpResponse] =
    MarketServiceHandler.withServerReflection(
      new MarketServiceImplSharding())

  val bindingFuture: Future[Http.ServerBinding] =
    Http().newServerAt("0.0.0.0", 9000).bind(service)

  val bindingFutureWallet =
    Http()
      .newServerAt("0.0.0.0", 9001)
      .bind(new WalletService().route)

  println(
    s"servers at localhost:9000 and localhost:9001 \nPress RETURN to stop")
  StdIn.readLine()
  val res = for {
    s1 <- bindingFuture
    u1 <- s1.unbind
    s2 <- bindingFutureWallet
    u2 <- s2.unbind
  } yield (u1, u2)

  res.onComplete(_ => system.terminate())
}
