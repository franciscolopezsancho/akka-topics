package example.bet.grpc

import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import scala.concurrent.{ ExecutionContext, Future }
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ HttpRequest, HttpResponse }

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
