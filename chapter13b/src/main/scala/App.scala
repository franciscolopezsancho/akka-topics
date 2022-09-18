package example.clustering2

import akka.management.scaladsl.AkkaManagement
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import com.typesafe.config.{ ConfigFactory }

object App {

  def main(args: Array[String]): Unit = {

    val system =
      ActorSystem[Nothing](
        Behaviors.empty,
        "testing-bootstrap13b",
        ConfigFactory.load())

    AkkaManagement(system).start()
    ClusterBootstrap(system).start()

  }
}
