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

import scala.io.StdIn

object Main {

  def main(args: Array[String]): Unit = {

    implicit val system =
      ActorSystem(Behaviors.empty, "betting-house")

    AkkaManagement(system).start()
    ClusterBootstrap(system).start()

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
  }

}
