package example.bet.grpc

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.management.scaladsl.AkkaManagement
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import scala.concurrent.{ ExecutionContext, Future }
import akka.http.scaladsl.{ Http, HttpConnectionContext }
import akka.http.scaladsl.model.{ HttpRequest, HttpResponse }
import com.typesafe.config.ConfigFactory

import example.bet.grpc.{ BetServiceHandler, BetServiceImplSharding }

object BetServiceServer {

  def init(
      implicit system: ActorSystem[_],
      sharding: ClusterSharding,
      ec: ExecutionContext): Future[Http.ServerBinding] = {
    val betService: HttpRequest => Future[HttpResponse] =
      BetServiceHandler.withServerReflection(
        new BetServiceImplSharding())

    val port = system.settings.config.getInt("services.bet.port")
    val host = system.settings.config.getString("services.host")

    Http().newServerAt(host, port).bind(betService)
  }

}
