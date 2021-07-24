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


object MyShardingCoordinator {

  sealed trait Command
  case class CreateRegion(name: String)
      extends Command
      with CborSerializable

  private val regions: ConcurrentHashMap[String, ActorRef[String]] =
    new ConcurrentHashMap

  def apply(): Behavior[Command] = {
    Behaviors.receive { (context, msg) =>
      msg match {
        case CreateRegion(name) =>
          val ref = context.spawnAnonymous(MyShardedRegion())
          regions.put(name, ref)
          context.log.info(s"added new ref. Current state $regions")
          Behaviors.same
      }
    }
  }
}

object MyShardedRegion {
  def apply(): Behavior[String] = Behaviors.ignore 
}
