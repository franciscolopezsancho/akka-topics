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

import scala.io.StdIn

import example.bet.akka.http.WalletServiceServer
import example.repository.scalike.BetRepositoryImpl
import betting.house.projection.{
  BetProjection,
  BetProjectionServer,
  MarketProjection
}

import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory
import scala.util.control.NonFatal
import example.repository.scalike.ScalikeJdbcSetup

object Main {

  val log = LoggerFactory.getLogger(Main + "")

  def main(args: Array[String]): Unit = {
    implicit val system =
      ActorSystem[Nothing](Behaviors.empty, "betting-house")
    try {

      implicit val sharding = ClusterSharding(system)
      implicit val ec: ExecutionContext = system.executionContext

      AkkaManagement(system).start()
      ClusterBootstrap(system).start()
      ScalikeJdbcSetup.init(system)

      BetServiceServer.init(system, sharding, ec)
      MarketServiceServer.init(system, sharding, ec)
      WalletServiceServer.init(system, sharding, ec)

      val betRepository = new BetRepositoryImpl()
      BetProjectionServer.init(betRepository)
      BetProjection.init(system, betRepository)
      MarketProjection.init(system)

      println(s"####################### application starting \nPress RETURN to stop")
      StdIn.readLine()
      system.terminate()
    } catch {
      case NonFatal(ex) =>
        log.error(
          s"Terminating Betting App. Reason [${ex.getMessage}]")
        system.terminate
    }

  }

}
