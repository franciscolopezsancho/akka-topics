package betting.house.projection

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.{ HttpRequest, HttpResponse }
import akka.http.scaladsl.Http
import betting.house.projection.proto.{
  BetProjectionServiceHandler,
  BetProjectionServiceImpl
}
import example.repository.scalike.BetRepository

import scala.concurrent.{ ExecutionContext, Future }

object BetProjectionServer {

  def init(repository: BetRepository)(
      implicit ec: ExecutionContext,
      system: ActorSystem[_]): Unit = {
    val service: HttpRequest => Future[HttpResponse] =
      BetProjectionServiceHandler.withServerReflection(
        new BetProjectionServiceImpl(system, repository))

    val port =
      system.settings.config.getInt("services.bet-projection.port")
    val host = system.settings.config.getString("services.host")

    Http().newServerAt(host, port).bind(service)

  }

}
