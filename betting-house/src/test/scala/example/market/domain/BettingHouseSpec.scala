package example.market

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

class BettingHouseSpec  extends ScalaTestWithActorTestKit(
      EventSourcedBehaviorTestKit.config.withFallback(
        ConfigFactory.load("application-test")))
    with AnyWordSpecLike
    with Matchers
    with LogCapturing {


  //   	Scenario 1
  // t0 - Fixture Real Madrid vs Manchester United. 
  // t1 - Market1 created before bet. 
  // ...
  // 	- Ann bets Market1 100 and gets accepted
  // 	- many people bet and odds seem move making RM more favourite than  
  // 	- Market2 created before bet. 
  // 	- Ben bets 100 and gets accepted
  // 	- Match starts MU scores
  // 	- Market 3 created MU favourite and odds to RM increase. 
  // 	- Match finishes
  // 	- Markets close
  // 	- Ann bets Market3 and gets rejected
  // 	- bets get resolved and pay off to users and betting houses. 

  "a persistent entity with sharding" should {

    "be able to add container" in {
      val sharding = ClusterSharding(system)

      val shardRegion
          : ActorRef[ShardingEnvelope[Market.Command]] =
        sharding.init(
          Entity(Market.TypeKey)(createBehavior = entityContext =>
            Market(entityContext.entityId)))

      val marketId = "id-1"

      val fixture = MarketActor.Fixtures(
      	id = "fixtureID1", 
      	sport = "football",
      	homeTeam = "ManchesterUnited", 
      	awayTeam = "RealMadrid")

      val odds = Odds(
      	winHome = 1.25L,
      	loseHome = 1.70L,
      	tie = 1.05)

	 val probe =
        createTestProbe[Market.Persisted]()

      shardRegion ! ShardingEnvelope(
        marketId,
        Market.Init(fixture, odds, probe.ref))

      probe.expectMessage(Persisted)

    }

  }
}