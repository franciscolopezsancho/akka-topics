package example.betting

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.management.scaladsl.AkkaManagement
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import scala.concurrent.{ ExecutionContext, Future }
import akka.http.scaladsl.{ Http, HttpConnectionContext }
import akka.http.scaladsl.model.{ HttpRequest, HttpResponse }

import example.market.grpc.{
  MarketServiceHandler,
  MarketServiceImplSharding
}

import example.bet.grpc.BetServiceServer

object MarketServiceServer {

  def init(
      implicit system: ActorSystem[_],
      sharding: ClusterSharding,
      ec: ExecutionContext): Future[Http.ServerBinding] = {
    val marketService: HttpRequest => Future[HttpResponse] =
      MarketServiceHandler.withServerReflection(
        new MarketServiceImplSharding())

    val port =
      system.settings.config.getInt("services.market.port")
    val host = system.settings.config.getString("services.host")

    Http().newServerAt(host, port).bind(marketService)
  }

}
