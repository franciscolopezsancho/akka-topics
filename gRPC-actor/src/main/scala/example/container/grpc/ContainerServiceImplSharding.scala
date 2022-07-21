package example.container.grpc

import example.container.grpc.{
  AddedCargo,
  Cargo,
  CargoEntity,
  Cargos,
  ContainerService,
  EntityId
}
import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.scaladsl.{
  ClusterSharding,
  Entity
}

import akka.util.Timeout

import example.container.domain.Container

import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration._

class ContainerServiceImplSharding(
    sharding: ClusterSharding)(
    implicit val executionContext: ExecutionContext)
    extends ContainerService {

  override def addCargo(in: CargoEntity): Future[AddedCargo] = {
    val container =
      sharding.entityRefFor(Container.TypeKey, in.entityId)

    container ! Container.AddCargo(Container.Cargo(in.kind, in.size))
    Future.successful(AddedCargo(true))
  }


  implicit val timeout: Timeout = 3.seconds

  override def getCargos(in: EntityId): Future[Cargos] = {
    val container =
      sharding.entityRefFor(Container.TypeKey, in.entityId)

    container
      .ask(Container.GetCargos)
      .mapTo[Container.Cargos]
      .map { containerCargos =>
        val c = containerCargos.cargos.map { each =>
          Cargo(each.kind, each.size)
        }
        Cargos(c)
      }
  }

}
