package betting.house.projection

import scala.util.control.NonFatal

import java.time.Instant

import org.slf4j.LoggerFactory

import com.typesafe.config.ConfigFactory

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors

import akka.http.scaladsl.model.{ HttpRequest, HttpResponse }
import akka.http.scaladsl.Http

import example.repository.scalike.BetRepositoryImpl
import example.repository.scalike.ScalikeJdbcSetup

import betting.house.projection.proto.{
  BetProjectionServiceHandler,
  BetProjectionServiceImpl
}
import example.repository.scalike.BetRepository

import scala.concurrent.{ ExecutionContext, Future }

object BetProjectionServer {

  val logger = LoggerFactory.getLogger(BetProjectionServer + "")

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
