package example.betting

import akka.actor.typed.{ ActorRef, Behavior, SupervisorStrategy}
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.sharding.typed.scaladsl.{Entity, EntityTypeKey, ClusterSharding}
import akka.cluster.sharding.typed.{ShardingEnvelope}

import akka.persistence.typed.scaladsl.{ReplyEffect, Effect, EventSourcedBehavior, RetentionCriteria }
import akka.persistence.typed.PersistenceId


import scala.concurrent.duration._
import scala.util.{Success, Failure}

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

  sealed trait Command {
    def replyTo: ActorRef[Response]
  }
  case class Open(walletId: String, marketId: String, odds: Int, stake: Int, replyTo: ActorRef[Response]) extends Command
  //probably want a local class not to depend on Market? see Market.State below
  case class Settle(marketInfo: Market.State, replyTo: ActorRef[Response]) extends Command
  case class Cancel(reason: String) extends Command
  private case class ValidationsTimedOut(seconds: Int) extends Command
  private case class ValidateMarket(marketId: String) extends Command
  private case class ValidateFunds(walletId: String, stake: Int) extends Command
  private case class WalletWrapper(response: Wallet.FundsReserved) extends Command

  //how do I know I bet to the winner or the looser or draw??
  case class Status(betId: String, walletId: String, odds: Int, stake: Int)
  object Status{
    def empty(marketId: String) = Status(marketId, "", -1, -1)
  }
  sealed trait State {
    def status: Status
  }



  case class UninitializedState(status: Status) extends State
  case class OpenState(status: Status, validations: Map[String, Option[Boolean]]) extends State// the ask user when market no longer available
  case class SettledState(status: Status) extends State
  case class CancelledState(status: Status) extends State
  case class FailedState(status: Status) extends State

  def apply(marketId: String): Behavior[Command] =
    Behaviors.withTimers{ timer => 
      timer.startSingleTimer("lifespan",ValidationsTimedOut(10), 10.seconds)
      Behaviors.setup { context => 
        val sharding = ClusterSharding(context.system)
    EventSourcedBehavior[Command, Event, State](
      PersistenceId(TypeKey.name, marketId),
      UninitializedState(Status.empty(marketId)),
      commandHandler = (state, command) => handleCommands(state, command, sharding),
      eventHandler = handleEvents
    )
  }
  }

  def handleCommands(state: State, command: Command, sharding: ClusterSharding): Effect[Event, State] = 
    (state, command) match {
    case (state: UninitializedState, command: Open) => open(state, command)
    case (state: OpenState, command: ValidateMarket) => validateMarket(state, command, sharding)
    case (state: OpenState, command: ValidateFunds) => validateFunds(state, command, sharding)
    case (state: OpenState, command: ValidationsTimedOut) => checkValidations(state, command)
    case (state: OpenState, command: Settle) => settle(state, command, sharding)
    case (_, command: CancelledState) => cancel(state, command)
    case _ => invalid(state, command)
  }

  sealed trait Event
  case class MarketValidated(betId: String, betOdds: Int, marketOdds: Int) extends Event
  case class FundsGranted(betId: String, walletId: String, stake: Int) extends Event
  case class ValidationsPassed(betId: String, validations: List[String]) extends Event
  case class Opened(betId: String, marketId: String, walletId: String, odds: Int, stake: Int) extends Event
  case class Settled(betId: String) extends Event
  case class Cancelled(betId: String, reason: String) extends Event
  case class Failed(betId: String, reason: String) extends Event


  def handleEvents(state: State, event: Event): State = event match {
    case Opened(betId, marketId, walletId, odds, stake) =>
      OpenState(state.status, Map(marketId -> None, walletId -> None))
    case MarketValidated(betId, betOdds, marketOdds) => 
      state
    case FundsGranted(betId, betOdds, marketOdds) => 
      state
    case Settled(betId) => 
      SettledState(state.status)
    case Cancelled(betId, reason) => 
      CancelledState(state.status)
    case Failed(betId, reason) => 
      FailedState(state.status)
  } 

  sealed trait Response
  case class Accepted(betId: String, command: Command) extends Response
  case class RequestUnaccepted(reason: String) extends Response
  case class MarketChanged(betId: String, newPrice: Int) extends Response

  def open(state: State, command: Open): ReplyEffect[Opened, State] = { 
    val open = Opened(state.status.betId, command.marketId, command.walletId, command.odds, command.stake)
    Effect.persist(open).thenReply(command.replyTo)(_ => Accepted(state.status.betId, command))
    //the send to itself ValidateMarket and ValidateFunds
  }

  //market changes very fast even if our system haven't register the
  //change we need to take this decision quickly. If the Market is not available
  // we fail fast.
  def validateMarket(state: OpenState, command: ValidateMarket, sharding: ClusterSharding): ReplyEffect[Opened, State] = {
  val market = sharding.entityRefFor(Market.TypeKey, command.marketId)
 
    market.ask(ShardingEnvelope(
        command.marketId, Market.GetState())) {
      case Success(Market.CurrentState(marketState)) =>
        if (checkOddsAvailable(marketState, state.status.odds)) {
          val opened = Opened(state.status.betId, command.marketId, state.status.walletId, state.status.odds, state.status.stake)
          Effect.persist(opened)
          //whoever is listening the Bet projection might find is Open
        } else { 
          val failed = Failed(state.status.betId, s"required odds [${state.status.odds}] but found market has  [${marketState.status.odds}]")
          Effect.persist(failed)
          //whoever is listening the Bet projection might find is still NOT Open
        }
      case Failure(ex) => 
          Effect.persist(Failed(state.status.betId, ex.getMessage()))
    }
  }

  //shall I use Shard or ShardRegion?
  // val walletShardRegion: ActorRef[ShardingEnvelope[Wallet.Command]] =
  //    sharding.init(Entity[Wallet.TypeKey])(createBehavior = entityContext => 
  //     Market(entityContext.entityId)) 


  /// if I already have asks why do I need a global time out?
      ///I could use that global time out and then indirectly let the Wallet grant the Bet otherwise will be cancelled. 
  //As this methods now requires me to create the Customer maybe is better leave it as a theoretical 
  // possibility
  //I can tell to funds and make the case I might need thids party calls or
  /// the wallet might need to do so and there fore multiple asks chained are 
  /// a bad practice. 
  /// I would need an adapter
  def validateFunds(state: OpenState, command: ValidateFunds, sharding: ClusterSharding): ReplyEffect[Event, State] = {
     val wallet = sharding.entityRefFor(Wallet.TypeKey, command.walletId)
     wallet ! ShardingEnvelope(
        command.walletId, Wallet.ReserveFunds(command.stake)) 
  }

  def checkValidations(state: OpenState, command: ValidationsTimedOut): Effect[Event, State] = {
    if (state.validations.values.forall(each => each.getOrElse(false))) 
      Effect.persist(ValidationsPassed(state.status.betId, state.validations.keys.toList))
    else 
      Effect.persist(Cancelled(state.status.betId, s"validations didn't passed [${state.validations}]"))
  }
 

  def checkOddsAvailable(marketState: Market.State, odds: Int): Boolean = {
    true
  }

  def checkFunds(stake: Int, fundsId: String): Boolean = {
    true
  }

  def isWinner(state: State, marketInfo: Market.State): Boolean = {
    true
  }

  def settle(
      state: State,
      command: Settle, sharding: ClusterSharding): Effect[Event, State] = {
    if (isWinner(state, command.marketInfo)) {
       val wallet = sharding.entityRefFor(Wallet.TypeKey, state.status.walletId)

        wallet.ask(ShardingEnvelope(
          command.marketInfo.status.marketId, Wallet.AddFunds(state.status.stake))) match {
          case Success(_) => 
             Effect.persist(Settled(state.status.betId))  
          case Failure(ex) => 
             Effect.persist(Failed(state.status.betId, ex.getMessage()))
        }
    } else {
      Effect.persist(Settled(state.status.betId))
    }
  }

  def cancel(
      state: State,
      command: Command): Effect[Event, State] = {
    command match {
      case ValidationsTimedOut(time) => Effect.persist(Cancelled(state.status.betId, s"validation in process when life span expired after [$time] seconds"))
    }
  }

  def invalid(
      state: State,
      command: Command): Effect[Event, State] = {
    Effect.none.thenReply(command.replyTo)(_ => RequestUnaccepted(s"[$command] is not allowed upon state [$state]"))
 }


}
