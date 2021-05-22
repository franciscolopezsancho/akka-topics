package example.persistence

import scala.util.control.NonFatal
import scala.annotation.tailrec
import scala.io.StdIn

import java.time.Instant

import org.slf4j.LoggerFactory

import akka.actor.typed.{ ActorRef, ActorSystem, Behavior }
import akka.actor.typed.scaladsl.Behaviors

import akka.cluster.sharding.typed.scaladsl.{
  ClusterSharding,
  Entity,
  EntityTypeKey
}
import akka.cluster.sharding.typed.ShardingEnvelope

import akka.cluster.sharding.typed.ShardedDaemonProcessSettings
import akka.cluster.sharding.typed.scaladsl.ShardedDaemonProcess

import example.persistence.CommandLine.Command

object PMain {

  //write
  val logger = LoggerFactory.getLogger(Main + "")

  def main(args: Array[String]): Unit = {
    val system = ActorSystem[Nothing](Behaviors.empty, "containers")
    try {
      val shardRegion = init(system)
      commandLoop(system, shardRegion)
    } catch {
      case NonFatal(ex) =>
        logger.error(s"terminating by NonFatal Exception", ex)
        system.terminate()
    }
  }

  def init(system: ActorSystem[_])
      : ActorRef[ShardingEnvelope[PContainer.Command]] = {
    val sharding = ClusterSharding(system)

    val shardRegion: ActorRef[ShardingEnvelope[PContainer.Command]] =
      sharding.init(
        Entity(PContainer.TypeKey)(createBehavior = entityContext =>
          PContainer(entityContext.entityId)))

    val containerId = "id-1"
    val cargo = PContainer.Cargo("id-c", "sack", 3)

    shardRegion

  }

  @tailrec
  private def commandLoop(
      system: ActorSystem[_],
      shardRegion: ActorRef[ShardingEnvelope[PContainer.Command]])
      : Unit = {

    print("please write:")
    val commandString = StdIn.readLine()

    if (commandString == null) {
      system.terminate()
    } else {
      Command(commandString) match {
        case Command.AddCargo(
            containerId,
            cargoId,
            cargoKind,
            cargoSize) =>
          shardRegion ! ShardingEnvelope(
            containerId,
            PContainer.AddCargo(
              PContainer.Cargo(cargoId, cargoKind, cargoSize)))

          commandLoop(system, shardRegion)
        case Command.Unknown(command) =>
          logger.warn("Unknown command {}!", command)
          commandLoop(system, shardRegion)
        case Command.Quit =>
          logger.info("Terminating by user signal")
          system.terminate
          commandLoop(system, shardRegion)
      }
    }
  }

}
