package example.sharding

import akka.actor.testkit.typed.scaladsl.{LogCapturing, ScalaTestWithActorTestKit}
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.matchers.should.Matchers
import akka.actor.typed.ActorRef
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityRef}
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

      container! Container.GetCargos(probe.ref)
      probe.expectMessage(List(cargo))

    }
  }
}


