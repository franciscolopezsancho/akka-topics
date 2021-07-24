package example.singleton

import akka.management.scaladsl.AkkaManagement
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.cluster.typed.{ ClusterSingleton, SingletonActor }
import akka.actor.typed.{
  ActorRef,
  ActorSystem,
  Behavior,
  SupervisorStrategy
}
import akka.actor.typed.scaladsl.Behaviors
import com.typesafe.config.{ ConfigFactory }

import java.util.concurrent.ConcurrentHashMap
import scala.util.Random

import example.singleton.MyShardingCoordinator

object App {

  def main(args: Array[String]): Unit = {

    val system =
      ActorSystem[Nothing](
        Behaviors.empty,
        "testing-singleton",
        ConfigFactory.load())

    AkkaManagement(system).start()
    ClusterBootstrap(system).start()

    val singletonFactory = ClusterSingleton(system)

    val myShardingCoordinator = singletonFactory.init(
      SingletonActor(
        Behaviors
          .supervise(MyShardingCoordinator())
          .onFailure(SupervisorStrategy.restart),
        "myShardingCoordinator"))
   
    myShardingCoordinator ! MyShardingCoordinator.CreateRegion(
        system.address.hostPort)
  }
}
