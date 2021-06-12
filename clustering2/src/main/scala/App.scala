package example.clustering2

import akka.management.scaladsl.AkkaManagement
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import com.typesafe.config.{ ConfigFactory }

object App {

  def main(args: Array[String]): Unit = {
    val i = args(0)

    val config = ConfigFactory
      .parseString(s"""
        akka.remote.artery.canonical.hostname = "127.0.0.$i"
        akka.management.http.hostname = "127.0.0.$i"
        """)
      .withFallback(ConfigFactory.load())

    val system =
      ActorSystem[Nothing](
        Behaviors.empty,
        "testing-bootstrap",
        config)

    AkkaManagement(system).start()
    ClusterBootstrap(system).start()

  }

}
