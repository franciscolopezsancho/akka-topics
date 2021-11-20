package example.betting

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

import java.time.OffsetDateTime

class BettingHouseSpec
    extends ScalaTestWithActorTestKit(
      EventSourcedBehaviorTestKit.config.withFallback(
        ConfigFactory.load("application-test")))
    with AnyWordSpecLike
    with Matchers
    with LogCapturing {

  //   	Scenario 1
  // t0 - Fixture Real Madrid vs Manchester United.
  // t1 - Market1 created.
  // ...
  // 	- Ann bets Market1 100 pro MU and gets accepted
  // 	- Match finishes and Ann wins
  // 	- Markets close
  // 	- Ann bets Market3 and gets rejected
  // 	- bets get resolved and pays off to users and betting houses.

  "a market" should {

    "get initialized" in {

      val market = EventSourcedBehaviorTestKit[
        Market.Command,
        Market.Event,
        Market.State](system, Market("id1"))

      val fixture = Market.Fixture(
        id = "fixtureID1",
        homeTeam = "ManchesterUnited",
        awayTeam = "RealMadrid")

      val odds =
        Market.Odds(winHome = 1.25, winAway = 1.70, draw = 1.05)

      val opensAt = OffsetDateTime.now();

      def auxInitialize(
          fixture: Market.Fixture,
          odds: Market.Odds,
          opensAt: OffsetDateTime)(
          replyTo: ActorRef[Market.Response]) =
        Market.Open(fixture, odds, opensAt, replyTo)

      val result = market.runCommand[Market.Response](
        auxInitialize(fixture, odds, opensAt))

      result.reply shouldBe (Market.Accepted)
    }

  }
  //     Scenario 1.1
  // t0 - Fixture Real Madrid vs Manchester United.
  // t1 - Market1 created.
  // ...
  //  - Markets close
  //  - Ann bets Market3 and gets rejected
  //     Scenario 2
  // t0 - Fixture Real Madrid vs Manchester United.
  // t1 - Market1 created.
  // ...
  //  - Ann bets before the event starts and the bets gets scheduled

  //     Scenario 3
  // t0 - Fixture Real Madrid vs Manchester United.
  // t1 - Market1 created.
  // ...
  //  - Ann bets before the event starts and the bets gets scheduled
  //  - The event starts
  //  - The bet checks the market and gets validated
  //     Scenario 4
  // t0 - Fixture Real Madrid vs Manchester United.
  // t1 - Market1 created.
  // ...
  //  - Ann bets after the event starts
  //  - The bet checks the market and gets validated
  //  - It doesn't pass the validation, the market has moved
  //  - It answers to the customer the new odd
  //  - it holds the bet, and waits for response.
  //  - the answer comes back and gets accepted

  //     Scenario 5
  // t0 - Fixture Real Madrid vs Manchester United.
  // t1 - Market1 created.
  // ...
  //  - Ann bets after the event starts
  //  - The bet checks the market and gets validated
  //  - It doesn't pass the validation, the market has moved
  //  - It answers to the customer the new odd
  //  - it holds the bet, and waits for response.
  //  - the answer comes back and gets rejected (the user doesn't want it)

  //     Scenario 6
  // t0 - Fixture Real Madrid vs Manchester United.
  // t1 - Market1 created.
  // ...
  //  - Ann bets after the event starts
  //  - The bet checks the market and gets validated
  //  - It doesn't pass the validation, the market has moved
  //  - It answers to the customer the new odd
  //  - it holds the bet, and waits for response.
  //  - the answer does NOT come back
  //  - the bet gets Rejected.

}
