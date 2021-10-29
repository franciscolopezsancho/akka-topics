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

object Bet {


  val TypeKey = EntityTypeKey[Command]("bet")

  sealed trait Command
  //how to avoid that they change result?
  case class CreateBet(marketId: String, userId: String, price: Int, stake: Int, replyTo: ActorRef[Response])
  case class OpenBet(marketId: String, newPrice: Int)
  case object Settle
  case object Cancel

  // when are do the events have different values than the Command?
  sealed trait Event
  case class Created(marketId: String, price: Int, stake: Int) extends Event
  case class Denied(marketId: String, replyTo: ActorRef[Response]) extends Event
  case class Accepted(marketId: String)

  sealed trait Response
  case object Accepted extends Response
  case class UnAcceptable(reason: String)
  case class MarketChanged(betId: String, newPrice: Int)
  //why is scheduling important if we can bet before started?


  case class Status(marketId: String, userId: String, price: Int, stake: Int)
  sealed trait State {
    def state: Status;
  }

  case class Initial(Status("","",0,0))
  case class Open(state: Status) extends State// the ask user when market no longer available
  case class Cancelled(state: Status) extends State
  case class Accepted(state: Status) extends State
  case class Reimbursing(state: Status) extends State
  //more states


  def apply(marketId: String): Behavior[Command] =
    EventSourcedBehavior[Command, Event, State](
      PersistenceId(TypeKey.name, marketId),
      Initial,
      commandHandler = handleCommands
      eventHandler = handleEvent
    )

  def handleCommands(state: State, command: Command): Effect[Event, State] = {
    state match {
      case Initial => validate(betId, state, command) 


    }
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


  def validate(containerId: String,
      state: State,
      command: Command) = {
    // checks the event has started otherwise sleeps
    // here we request the market still has that price
    // and check with third service that the user didn't consume too much
  }

  def reimbursement(
      containerId: String,
      state: State,
      command: Command): Effect[Event, State] = ???

  def handleEvent(state: State, event: Event): State = ???


}
