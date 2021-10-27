package example.mscontainer.grpc

import akka.actor.typed.{ ActorRef, Behavior, SupervisorStrategy}
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.sharding.typed.scaladsl.EntityTypeKey

import akka.persistence.typed.scaladsl.{ Effect, EventSourcedBehavior, RetentionCriteria }
import akka.persistence.typed.PersistenceId


import scala.concurrent.duration._




/**
 * Lifecycle of a bet
 * Some user finds, looking at a market projection, a good price and decides to make a bet with those odds
 * Or rather the user decides any odds and then this checks against the market.
 *    Q -> how the projection of the market affect its odds 
 * Q -> is it necessary somebody takes the bet? meaning I bet to market1, 10$, at 1.25. Who is betting against? so this bet can be paid if its counterpart looses?
 *
 * Q -> if the states are prematch, checking, active, cancelled and reimbursement. What changes/ messages do I want to avoid with these states?
 *
 *
 * Q -> to awake or pay every Bet do we rely on the recepcionist?
 *
 * 
 *
 *
 */

object BetActor {


  val TypeKey = EntityTypeKey[Command]("bet")

  sealed trait Command
  //how to avoid that they change result?
  case class Bet(marketId: String, userId: String, price: int32, stake: int32, replyTo: ActorRef[Response])


  // when are do the events have different values than the Command?
  sealed trait Event
  case class BetProcessed(marketId: String, price: int32, stake: int32) extends Event
  case class BetDenied(marketId: String, replyTo: ActorRef[Response])

  sealed trait Response
  case object Accepted extends Response
  case class UnAcceptable(reason: String)
  //why is scheduling important if we can bet before started?

  case class State(marketId: String, userId: String, price: int32, stake: int32, scheduled: Int, open: Boolean, fixture: Fixture, accepted: boolean)



  def apply(marketId: String): Behavior[Command] =
    Behaviors.withTimers { timer => 
      timer.startTimerAtFixedRate("late",CheckInSchedule,1.minute)
    EventSourcedBehavior[Command, Event, State](
      PersistenceId(TypeKey.name, marketId),
      State(None, None,None),
      commandHandler = handleCommands
      eventHandler = handleEvent
    )

  def handleCommands(state: State, command: Command): Effect[Event, State] = {
    if (state.open) => active(marketId, state, command)
    else closed(marketId, state, command)
  }

  def active(
      containerId: String,
      state: State,
      command: Command): Effect[Event, State] =
    command match {
      case Bet(marketId, price, stake, replyTo) =>
        //check 
        Effect.persist(BetAccepted(fixture, odds, result)).thenRun(replyTo ! BetPersisted)
    }

  def scheduled(containerId: String,
      state: State,
      command: Command) = ???
  def checkingValidation(containerId: String,
      state: State,
      command: Command) = ???// what are we trying to avoid here? what message we don't want to process?

  def paying(
      containerId: String,
      state: State,
      command: Command): Effect[Event, State] = ???

  def handleEvent(state: State, event: Event): State = ???


}
