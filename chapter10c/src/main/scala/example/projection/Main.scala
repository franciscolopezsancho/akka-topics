package example.projection

import scala.util.control.NonFatal

import java.time.Instant

import org.slf4j.LoggerFactory

import com.typesafe.config.ConfigFactory

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors

import akka.cluster.sharding.typed.ShardedDaemonProcessSettings
import akka.cluster.sharding.typed.scaladsl.ShardedDaemonProcess

import akka.projection.ProjectionBehavior

import example.repository.scalike.CargosPerContainerRepositoryImpl
import example.repository.scalike.ScalikeJdbcSetup

object Main {

  val logger = LoggerFactory.getLogger(Main + "")

  def main(args: Array[String]): Unit = {
    logger.info("initializing system")
    val system = if (args.isEmpty) {
      initActorSystem(0)
    } else {
      initActorSystem(args(0).toInt)
    }

    try {
      ScalikeJdbcSetup.init(system)
      initProjection(system)
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
    ActorSystem[Nothing](
      Behaviors.empty,
      "containersprojection",
      config)
  }

  def initProjection(system: ActorSystem[_]): Unit = {
    ShardedDaemonProcess(system).init(
      name = "cargos-per-container-projection",
      3,
      index =>
        ProjectionBehavior(
          CargosPerContainerProjection.createProjectionFor(
            system,
            new CargosPerContainerRepositoryImpl(),
            index)),
      ShardedDaemonProcessSettings(system),
      Some(ProjectionBehavior.Stop))
  }

}
