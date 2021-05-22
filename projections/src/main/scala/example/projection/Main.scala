package example.projection

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

import example.persistence.PContainer
import example.repository.scalike.CargosPerContainerRepositoryImpl
import example.repository.scalike.ScalikeJdbcSetup
object Main {

  //write
  val logger = LoggerFactory.getLogger(Main + "")

  def main(args: Array[String]): Unit = {
    logger.info("initializing system")
    val system = ActorSystem[Nothing](Behaviors.empty, "postoffice")
    ScalikeJdbcSetup.init(system)
    try {
      val shardRegion = init(system)
    } catch {
      case NonFatal(ex) =>
        logger.error(s"terminating by NonFatal Exception", ex)
        system.terminate()
    }
  }

  def init(system: ActorSystem[_])
      : ActorRef[ShardingEnvelope[PContainer.Command]] = {
    logger.info("initializing sharded cluster")
    val shardRegion = ClusterSharding(system).init(
      Entity(PContainer.TypeKey)(entityContext =>
        PContainer(entityContext.entityId)))

    logger.info("initializing projection")
    // val projection = VisitedCitiesProjection.createProjectionFor(
    //   system,
    //   new VisitedCitiesRepositoryDBImpl(),1)

    // val projectionBehavior: Behavior[ProjectionBehavior.Command] = ProjectionBehavior(projection)
    // val projectionTypeKey = EntityTypeKey[ProjectionBehavior.Command]("projections-type-key")
    // val anotherRegion = ClusterSharding(system).init(
    //   Entity(projectionTypeKey)( entityContext => projectionBehavior))

    ShardedDaemonProcess(system).init(
      name = "whatever",
      3,
      index =>
        ProjectionBehavior(
          CargosPerContainerProjection.createProjectionFor(
            system,
            new CargosPerContainerRepositoryImpl(),
            index)),
      ShardedDaemonProcessSettings(system),
      Some(ProjectionBehavior.Stop))

    shardRegion

  }

}
