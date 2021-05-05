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

import akka.projection.ProjectionBehavior

import example.projection.VisitedCitiesProjection
import example.persistence.CommandLine.Command

object Script {

  //write
  val logger = LoggerFactory.getLogger(Script + "")

  def main(args: Array[String]): Unit = {
    logger.info("initializing system")
    val system = ActorSystem[Nothing](Behaviors.empty, "post-office")
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
      : ActorRef[ShardingEnvelope[Parcel.Command]] = {
    logger.info("initializing sharded cluster")
    val shardRegion = ClusterSharding(system).init(
      Entity(Parcel.TypeKey)(
        entityContext =>
          Parcel(
            entityContext.entityId,
            Parcel.calculateTag(entityContext))))

    logger.info("initializing projection")
    // val projection = VisitedCitiesProjection.createProjectionFor(
    //   system,
    //   new VisitedCitiesRepositoryDBImpl(),1)

    // val projectionBehavior: Behavior[ProjectionBehavior.Command] = ProjectionBehavior(projection)
    // val projectionTypeKey = EntityTypeKey[ProjectionBehavior.Command]("projections-type-key")
    // val anotherRegion = ClusterSharding(system).init(
    //   Entity(projectionTypeKey)( entityContext => projectionBehavior))

    ScalikeJdbcSetup.init(system)

    ShardedDaemonProcess(system).init(
      name = "whatever",
      Parcel.tags.size,
      index => ProjectionBehavior(VisitedCitiesProjection.createProjectionFor(
      system,
      new VisitedCitiesRepositoryDBImpl(),index)),
      ShardedDaemonProcessSettings(system),
      Some(ProjectionBehavior.Stop))

    shardRegion

  }

  @tailrec
  private def commandLoop(
      system: ActorSystem[_],
      shardRegion: ActorRef[ShardingEnvelope[Parcel.Command]])
      : Unit = {
        print("please write:")
    val commandString = StdIn.readLine()

    if (commandString == null) {
      system.terminate()
    } else {
      Command(commandString) match {
        case Command.Destination(parcelId, city, isFinal) =>
          shardRegion ! ShardingEnvelope(
            parcelId,
            Parcel.AddDestination(
              Parcel.Location(city, "bla", "bla"),
              isFinal))
          commandLoop(system, shardRegion)
        case Command.HandOver(parcelId, city) =>
          shardRegion ! ShardingEnvelope(
            parcelId,
            Parcel.HandOver(
              Parcel.Location(city, "bla", "bla"),
              Instant.now()))
          commandLoop(system, shardRegion)
        case Command.Unknown(command) =>
          logger.warn("Unknown command {}!", command)
          commandLoop(system, shardRegion)
      }
    }
  }

}
