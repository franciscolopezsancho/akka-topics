package example.projection

import scala.util.control.NonFatal

import java.time.Instant

import org.slf4j.LoggerFactory

import com.typesafe.config.ConfigFactory

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors

import akka.cluster.sharding.typed.ShardedDaemonProcessSettings
import akka.cluster.sharding.typed.scaladsl.ShardedDaemonProcess

import akka.http.scaladsl.model.{ HttpRequest, HttpResponse }
import akka.http.scaladsl.Http

import akka.projection.ProjectionBehavior

import example.repository.scalike.BetRepositoryImpl
import example.repository.scalike.ScalikeJdbcSetup

import example.bet.grpc.{ BetServiceHandler, BetServiceImpl }
import example.repository.scalike.BetRepository

import scala.concurrent.{ ExecutionContext, Future }
import scala.io.StdIn

object Main {

  val logger = LoggerFactory.getLogger(Main + "")

  def main(args: Array[String]): Unit = {
    logger.info("initializing system")
    val system = if (args.isEmpty) {
      initActorSystem(25533)
    } else {
      initActorSystem(args(0).toInt)
    }

    try {
      ScalikeJdbcSetup.init(system)
      val repository = new BetRepositoryImpl()
      initProjection(system, repository)
      initServer(repository)(ExecutionContext.global, system)
    } catch {
      case NonFatal(ex) =>
        logger.error(s"terminating by NonFatal Exception", ex)
        system.terminate()
    }
  }

  def initActorSystem(port: Int): ActorSystem[Nothing] = {
    val config = ConfigFactory
      .parseString(s"""
      akka.remote.artery.canonical.port=$port
      """)
      .withFallback(ConfigFactory.load())
    ActorSystem[Nothing](Behaviors.empty, "bet-projection", config)
  }

  def initProjection(
      system: ActorSystem[_],
      repository: BetRepository): Unit = {
    ShardedDaemonProcess(system).init(
      name = "bet-projection",
      3,
      index =>
        ProjectionBehavior(
          BetProjection
            .createProjectionFor(system, repository, index)),
      ShardedDaemonProcessSettings(system),
      Some(ProjectionBehavior.Stop))
  }

  def initServer(repository: BetRepository)(
      implicit ec: ExecutionContext,
      system: ActorSystem[_]): Unit = {
    val service: HttpRequest => Future[HttpResponse] =
      BetServiceHandler.withServerReflection(
        new BetServiceImpl(system, repository))

    val bindingFuture: Future[Http.ServerBinding] =
      Http().newServerAt("0.0.0.0", 9004).bind(service)

    println(s"servers at localhost:9004\nPress RETURN to stop")
    StdIn.readLine()

    bindingFuture
      .map(_.unbind)
      .onComplete(_ => system.terminate())

  }

}
