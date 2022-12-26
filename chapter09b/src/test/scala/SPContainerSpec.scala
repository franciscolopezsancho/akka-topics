package example.persistence

import akka.actor.testkit.typed.scaladsl.{
  LogCapturing,
  ScalaTestWithActorTestKit
}

import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.matchers.should.Matchers

import akka.persistence.testkit.scaladsl.EventSourcedBehaviorTestKit

import com.typesafe.config.ConfigFactory

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorRef, Behavior }

import akka.persistence.typed.scaladsl.{
  Effect,
  EventSourcedBehavior
}
import akka.persistence.typed.PersistenceId

import akka.cluster.sharding.typed.scaladsl.{
  ClusterSharding,
  Entity,
  EntityRef,
  EntityTypeKey
}
import akka.cluster.sharding.typed.ShardingEnvelope

class SPContainerSpec
    extends ScalaTestWithActorTestKit(
      EventSourcedBehaviorTestKit.config.withFallback(
        ConfigFactory.load("application-test")))
    with AnyWordSpecLike
    with Matchers
    with LogCapturing {

  "a persistent entity with sharding" should {

    "be able to add container" in {
      val sharding = ClusterSharding(system)
      val entityDefinition =
        Entity(SPContainer.typeKey)(createBehavior = entityContext =>
          SPContainer(entityContext.entityId))
      val shardRegion
          : ActorRef[ShardingEnvelope[SPContainer.Command]] =
        sharding.init(entityDefinition)

      val containerId = "123"
      val cargo = SPContainer.Cargo("id-c", "sack", 3)

      shardRegion ! ShardingEnvelope(
        containerId,
        SPContainer.AddCargo(cargo))

      val probe =
        createTestProbe[List[SPContainer.Cargo]]()
      val container: EntityRef[SPContainer.Command] =
        sharding.entityRefFor(SPContainer.typeKey, containerId)
      container ! SPContainer.GetCargos(probe.ref)

      probe.expectMessage(List(cargo))

    }

  }
}
