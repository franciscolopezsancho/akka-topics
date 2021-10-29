package example.mscontainer.grpc

import akka.actor.typed.{ ActorRef, Behavior, SupervisorStrategy}
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.sharding.typed.scaladsl.EntityTypeKey

import akka.persistence.typed.scaladsl.{ Effect, EventSourcedBehavior, RetentionCriteria }
import akka.persistence.typed.PersistenceId


import scala.concurrent.duration._
import java.util.OffsetDateTime


/**
 * 
 * Q - is there more markets? or more Bets 
 * Q - do we use a projection from bets to move the odds?? 
 * 
 */

object MarketActor {


  val TypeKey = EntityTypeKey[Command]("market")

  case class Fixture(id: String, sport: String, homeTeam: String, awayTeam: String)
  case class Odds(winHome: Int, loseHome: Int, tie: Int) //Q - its total sum must be 2?

  sealed trait Command
  case class Create(fixture: Fixture, odds: Odds, replyTo: ActorRef[Persisted]) extends Command
  case class Update(odds: Odds, replyTo: ActorRef[Persisted]) extends Command
  case class GetState(replyTo: ActorRef[State]) extends Command
  case object Close extends Command
  // case class Resolution() in cases when a Jury needs to review the result. Like in horse races. 
  case object Suspend(marketId: String, reason: String) extends Command // while a goal scores and odds needs to be recalculated
  case object Resume(marketId: String, reason: String ) extends Command

  sealed trait Event
  case class OddsUpdated(fixture: Fixture, odds: Odds) extends Event
  case class Closed(marketId: String, reason: String, at: OffsetDateTime, replyTo: ActorRef[Response])
  // case class Resolved()
  case class Suspended(marketId: String, reason: String, at: OffsetDateTime, replyTo: ActorRef[Response])
  case class Resumed(marketId: String, reason: String, at: OffsetDateTime, replyTo: ActorRef[Response])

  sealed trait Response
  case class UpdatedResponse(marketId: String) extends Response
  case class ClosedResponse(marketId: String) extends Response
  case class SuspendedResponse(marketId: String) extends Response
  case class ResumedResponse(marketId: String) extends Response
  case class CreatedResponse(marketId: String) extends Response
  case class CurrentState(state: State) // shall I use resposne
  //why is scheduling important if we can bet before started?

  case class Status(marketId: String, fixture: Fixture, odds: Odds)
  sealed trait State {
    def state: Status;
  }
  case class Open(state: Status) extends State
  case class Closed(state: Status) extends State
  case class Suspended(state: Status) extends State

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
