package example.mscontainer.grpc

import akka.actor.typed.{ ActorRef, Behavior, SupervisorStrategy}
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.sharding.typed.scaladsl.EntityTypeKey

import akka.persistence.typed.scaladsl.{ Effect, EventSourcedBehavior, RetentionCriteria }
import akka.persistence.typed.PersistenceId


import scala.concurrent.duration._


/**
 * 
 * Q - is there more markets? or more Bets 
 * 
 */

object MarketActor {


  val TypeKey = EntityTypeKey[Command]("market")

  case class Fixture(id: String, sport: String, homeTeam: String, awayTeam: String)
  case class Odds(winHome: Int, loseHome: Int, tie: Int) //Q - its total sum must be 2?

  sealed trait Command
  //how to avoid that they change result?
  case class Init(fixture: Fixture, odds: Odds, replyTo: ActorRef[Persisted])
  case class UpdateOdds(odds: Odds, replyTo: ActorRef[Persisted])
  case class GetState(replyTo: ActorRef[State])

  sealed trait Event
  case class OddsUpdated(fixture: Fixture, odds: Odds) extends Event

  sealed trait Response
  case object Persisted extends Response
  case class CurrentState(state: State) // shall I use resposne
  //why is scheduling important if we can bet before started?

  case class State(scheduled: Int, open: Boolean, fixture: Fixture, odds: Odds)

  def apply(marketId: String): Behavior[Command] =
    EventSourcedBehavior[Command, Event, State](
      PersistenceId(TypeKey.name, marketId),
      State(None, None,None),
      commandHandler = (state, command) =>
        if (state.open) open(marketId, state, command)
        else closed(marketId, state, command)
      eventHandler)

  def open(
      containerId: String,
      state: State,
      command: Command): Effect[Event, State] = ???
    

  def closed(
      containerId: String,
      state: State,
      command: Command): Effect[Event, State] = ???

  def eventHandler(state: State, event: Event): State = ???


}
