package example.container.grpc

import example.container.grpc.{
  CargoEntity,
  ContainerService,
  SizeLeft
}
import akka.actor.typed.{ ActorRef, ActorSystem }
import akka.cluster.sharding.typed.scaladsl.{
  ClusterSharding,
  Entity,
  EntityRef
}

import akka.stream.scaladsl.{ Flow, Sink, Source }
import akka.stream.typed.scaladsl.ActorFlow
import akka.NotUsed

import akka.util.Timeout

import example.container.domain.Container

import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration._

class ContainerServiceImplSharding(
    implicit system: ActorSystem[Nothing])
    extends ContainerService {

  implicit val timeout: Timeout = 3.seconds
  implicit val executionContext: ExecutionContext =
    system.executionContext

  val sharding = ClusterSharding(system)

  val shardingRegion =
    sharding.init(Entity(Container.TypeKey)(entityContext =>
      Container(entityContext.entityId)))

  def askFlow(
      ref: EntityRef[Container.Command],
      cargo: CargoEntity): Flow[String, Int, NotUsed] =
    ActorFlow.ask(ref)(makeMessage = (el, replyTo: ActorRef[Int]) =>
      Container
        .AddCargo(Container.Cargo(cargo.kind, cargo.size), replyTo))

  override def addCargo(in: Source[CargoEntity, akka.NotUsed])
      : Source[SizeLeft, akka.NotUsed] = {
    in.mapConcat { cargoEntity =>
        val containerRef: ActorRef[Container.Command] =
          sharding.entityRefFor(
            Container.TypeKey,
            cargoEntity.entityId)
        (containerRef, cargoEntity)
      }
      .map((containerRef, cargoEntity) =>
        askFlow(containerRef, cargoEntity))
      .runWith(Sink.seq)

  }

}
