package example.sharding

import akka.actor.testkit.typed.scaladsl.{
  LogCapturing,
  ScalaTestWithActorTestKit
}

import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.matchers.should.Matchers

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorRef, Behavior }

import akka.cluster.sharding.typed.scaladsl.{
  ClusterSharding,
  Entity,
  EntityRef,
  EntityTypeKey
}
import akka.cluster.sharding.typed.ShardingEnvelope

class ContainerSpec
    extends ScalaTestWithActorTestKit
    with AnyWordSpecLike
    with Matchers
    with LogCapturing {

  "a sharded freight entity" should {
    "be able to add a cargo" in {

      val sharding = ClusterSharding(system)

      val shardRegion: ActorRef[ShardingEnvelope[Container.Command]] =
        sharding.init(
          Entity(Container.TypeKey)(createBehavior = entityContext =>
            Container(entityContext.entityId)))

      val containerId = "id-1"
      val cargo = Container.Cargo("id-c", "sack", 3)

      shardRegion ! ShardingEnvelope(
        containerId,
        Container.AddCargo(cargo))

      val probe = createTestProbe[List[Container.Cargo]]()

      val container: EntityRef[Container.Command] =
        sharding.entityRefFor(Container.TypeKey, containerId)

      shardRegion ! ShardingEnvelope(
        containerId,
        Container.GetCargos(probe.ref))
      probe.expectMessage(List(cargo))

    }
  }
}

object Container {

  val TypeKey =
    EntityTypeKey[Container.Command]("container-type-key")

  final case class Cargo(id: String, kind: String, size: Int)

  sealed trait Command
  final case class AddCargo(cargo: Cargo)
      extends Command
      with CborSerializable
  final case class GetCargos(replyTo: ActorRef[List[Cargo]])
      extends Command
      with CborSerializable

  def apply(containerId: String): Behavior[Command] = {
    ready(List())
  }

  def ready(cargos: List[Cargo]): Behavior[Command] = {
    Behaviors.receiveMessage[Command] {
      case AddCargo(cargo) =>
        ready(cargo +: cargos)
      case GetCargos(replyTo) =>
        replyTo ! cargos
        Behaviors.same
    }
  }
}
