package example.shardeddeamon


import org.slf4j.LoggerFactory

import com.typesafe.config.ConfigFactory

import akka.actor.typed.{ActorSystem,Behavior}
import akka.actor.typed.scaladsl.Behaviors

import akka.cluster.sharding.typed.scaladsl.ShardedDaemonProcess

object MiniCluster {

  val logger = LoggerFactory.getLogger("App")

  def main(args: Array[String]): Unit = {
    if (args.isEmpty) {
      startup(25251)
    }else{
      startup(args(0).toInt)
    }
  }

  def startup(port: Int): Unit = {
    logger.info("starting cluster on port {}",port)
    val config = ConfigFactory
      .parseString(s"""
      akka.remote.artery.canonical.port=$port
      """)
      .withFallback(ConfigFactory.load("shardeddeamon"))

    val system = ActorSystem[Nothing](Behaviors.empty, "MiniCluster", config)

    val tags = Vector("tag-1","tag-2","tag-3")

    ShardedDaemonProcess(system).init("loggers",tags.size, index => LoggerBehavior(tags(index)))
  }
}

object LoggerBehavior {
  def apply(tag: String): Behavior[Unit] = {
    Behaviors.setup { context => 
      context.log.info("spawned LoggerBehavior {}",tag)
      Behaviors.receive {
        case _ => Behaviors.same
      }
    }
  }
}