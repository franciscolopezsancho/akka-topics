package example.shardeddeamon

import org.slf4j.LoggerFactory

import com.typesafe.config.ConfigFactory

import akka.actor.typed.{ ActorSystem, Behavior }
import akka.actor.typed.scaladsl.Behaviors

import akka.cluster.sharding.typed.scaladsl.ShardedDaemonProcess

object LoggerShardedApp {

  val logger = LoggerFactory.getLogger("App")

  def main(args: Array[String]): Unit = {
    startup(args(0).toInt)
  }

  def startup(port: Int): Unit = {
    logger.info("starting cluster on port {}", port)
    val config = ConfigFactory
      .parseString(s"""
      akka.remote.artery.canonical.port=$port
      """)
      .withFallback(ConfigFactory.load("shardeddeamon"))

    val system =
      ActorSystem[Nothing](Behaviors.empty, "LoggerSharded", config)

    val tags =
      Vector("container-tag-1", "container-tag-2", "container-tag-3")

    ShardedDaemonProcess(system).init(
      "loggers",
      tags.size,
      index => LoggerBehavior(tags(index)))
  }
}

object LoggerBehavior {
  def apply(tag: String): Behavior[Unit] = {
    Behaviors.setup { context =>
      context.log.info("spawned LoggerBehavior {}", tag)
      Behaviors.ignore
    }
  }
}
