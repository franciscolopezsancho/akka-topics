package betting.market.actor

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

object Market {


  val TypeKey = EntityTypeKey[Command]("market")

  case class Fixture(id: String, sport: String, homeTeam: String, awayTeam: String)
  case class Odds(winHome: Int, loseHome: Int, tie: Int) //Q - its total sum must be 2?

  sealed trait Command {
    def replyTo: ActorRef[Event]
  }
  //Can I delete marketId
  case class Initialize(fixture: Fixture, odds: Odds, opensAt: OffsetDateTime, replyTo: ActorRef[Created]) extends Command
  case class Update(odds: Odds, opensAt: OffsetDateTime,  replyTo: ActorRef[Updated]) extends Command
  case class Open(replyTo: ActorRef[])
  case object Close(replyTo: ActorRef[ClosedReponse]) extends Command
  case object Suspend(marketId: String, reason: String) extends Command // while a goal scores and odds needs to be recalculated
  case object Resume(marketId: String, reason: String) extends Command
  case object Cancel(marketId: String, reason: String) extends Command
  case class GetState(replyTo: ActorRef[State]) extends Command
  // case class Resolution() in cases when a Jury needs to review the result. Like in horse races. 


  sealed trait Event
  case class Initialized(marketId: String, fixture: Fixture, odds: Odds) extends Event
  case class OddsUpdated(marketId: String, fixture: Fixture, odds: Odds) extends Event
  case class Closed(marketId: String, reason: String, at: OffsetDateTime, replyTo: ActorRef[Response]) extends Event
  // case class Resolved() // if it doesn't apply to football might be better not to add it
  case class Suspended(marketId: String, reason: String, at: OffsetDateTime, replyTo: ActorRef[Response]) extends Event
  case class Resumed(marketId: String, reason: String, at: OffsetDateTime, replyTo: ActorRef[Response]) extends Event

  sealed trait Response
  case class Accepted(marketId: String, command: Command) extends Response
  case class CurrentState(state: State) extends Response
  case class RequestUnaccepted(reason: String) extends Response
  //why is scheduling important if we can bet before started? -> to open the market

  
  sealed trait State {
    def status: Status;
  }
  private case class Status(marketId: String, fixture: Fixture, odds: Odds)

  case class Uninitialized(state: Status) extends State
  case class InitializedState(state: Status) extends State
  case class OpenState(state: Status) extends State
  case class ClosedState(state: Status) extends State
  case class SuspendedState(state: Status) extends State

  def apply(marketId: String): Behavior[Command] =
    EventSourcedBehavior[Command, Event, State](
      PersistenceId(TypeKey.name, marketId),
      State(None, None,None),
      commandHandler = handleCommands
      eventHandler = handleEvents)


  def handleCommands(state: State, command: Command): ReplyEffect[Response] = { 
    case (state: Uninitialized, commmand: Initialize) => init(state, command)
    case (state: Initialized, command: Update) => update(state, command) 
    case (state: Initialized, command: Open) => open(state, command)
    case (state: OpenState, command: Update) => update(state, command) 
    case (state: OpenState, command: Suspend) => suspend(state, command)
    case (state: Suspend, command: Resume) => resume(state, command)
    case (state: OpenState, command: Close) => close(state, command)
    case (state: _, command: Cancel) => cancel(state, command)
    case (state: _, command: GetState) => tellState(state, command)
    case (state, command) => 
      Effect.reply(command.replyTo)(_ => RequestUnaccepted(s"[$command] is not allowed upon state [$state]"))
  }

  def init(state: State, command: Created): ReplyEffect[Response] = { 
    val created = Created(state.marketId, command.fixture, command.odds)
    Effect.persist(created).thenReply(command.replyTo)(_ => Accepted(state.marketId, command))
  }

  def update(state: State, command: Update): ReplyEffect[Response] = { 
    val updated = Updated(state.marketId,command.odds)
    Effect.persist(updated).thenReply(command.replyTo)(_ => Accepted(state.marketId, command))
  }

  def open(state: State, command: Open): ReplyEffect[Response] = { 
    Effect.persist(Opened()).thenReply(command.replyTo)(_ => Accepted(state.marketId, command))
  }

  def suspend(state: State, command: Suspend): ReplyEffect[Response] = { 
    val suspended = Suspended(state.marketId, command.reason)
    Effect.persist(suspended).thenReply(command.replyTo)(_ => Accepted(state.marketId, command))
  }

  def resume(state: State, command: Suspend): ReplyEffect[Response] = { 
    val resumed = Resumed(state.marketId, command.reason)
    Effect.persist(resumed).thenReply(command.replyTo)(_ => Accepted(state.marketId, command))
  }

  def close(state: State, command: Suspend): ReplyEffect[Response] = { 
    val closed = Closed(state.marketId)
    Effect.persist(closed).thenReply(command.replyTo)(_ => Accepted(state.marketId, command))
  }

  def cancel(state: State, command: Suspend): ReplyEffect[Response] = { 
    val cancelled = Cancelled(state.marketId, command.reason)
    Effect.persist(cancelled).thenReply(command.replyTo)(_ => Accepted(state.marketId, command))
  }

  def tellState(state: State, command: Created): ReplyEffect[Response] = { 
    Effect.reply(command.replyTo)(_ => CurrentState(state))
  }


  def handleEvents(state: State, event: Event): State = {
    event match {
      case Initialized(marketId, fixture, odds) => 
         Initialized(Status(marketId, fixture, odds)))
      case Updated(marketId, fixture, odds) => 
         state.copy(status = Status(marketId, fixture, odds)))
      case Open(marketId, fixture, odds) => 
         OpenState(marketId, fixture, odds)
      case Suspended(marketId, fixture, odds) => 
         SuspendedState(marketId, fixture, odds)
      case Resumed(marketId, fixture, odds) => 
         ResumedState(marketId, fixture, odds)
      case Closed(marketId, fixture, odds) => 
         ClosedState(marketId, fixture, odds)
      case Cancelled(marketId, fixture, odds) => 
         CancelledState(marketId, fixture, odds)
    }
  } 


}
