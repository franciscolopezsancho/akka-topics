package example.betting

import akka.actor.typed.{ ActorRef, Behavior, SupervisorStrategy}
import akka.actor.typed.scaladsl.{Behaviors, ActorContext}
import akka.cluster.sharding.typed.scaladsl.{Entity, EntityTypeKey, ClusterSharding}
import akka.cluster.sharding.typed.{ShardingEnvelope}

import akka.persistence.typed.scaladsl.{ReplyEffect, Effect, EventSourcedBehavior, RetentionCriteria }
import akka.persistence.typed.PersistenceId

import scala.concurrent.{Future, ExecutionContext}
import scala.concurrent.duration._
import scala.util.{Success, Failure}
import akka.util.Timeout

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
  trait ReplyCommand extends Command {
    def replyTo: ActorRef[Response]
  }
  case class Open(walletId: String, marketId: String, odds: Int, stake: Int, replyTo: ActorRef[Response]) extends ReplyCommand
  //probably want a local class not to depend on Market? see Market.State below
  case class Settle(marketInfo: Market.State, replyTo: ActorRef[Response]) extends ReplyCommand
  case class Cancel(reason: String, replyTo: ActorRef[Response]) extends ReplyCommand
  private case class CheckMarketOdds(available: Boolean) extends Command
  private case class RequestWalletFunds(response: Wallet.UpdatedResponse) extends Command
  private case class ValidationsTimedOut(seconds: Int) extends Command
  private case class ManualSettle() extends Command
  private case class Close(reason: String) extends Command


  //how do I know I bet to the winner or the looser or draw??
  case class Status(betId: String, walletId: String, odds: Int, stake: Int)
  object Status{
    def empty(marketId: String) = Status(marketId, "", -1, -1)
  }
  sealed trait State {
    def status: Status
  }
  case class UninitializedState(status: Status) extends State
  case class OpenState(status: Status, marketConfirmed: Option[Boolean] = None, fundsConfirmed: Option[Boolean] = None) extends State// the ask user when market no longer available
  case class SettledState(status: Status) extends State
  case class CancelledState(status: Status) extends State
  case class FailedState(status: Status) extends State
  case class ClosedState(status: Status) extends State


  def apply(marketId: String): Behavior[Command] = {
    Behaviors.withTimers{ timer => 
      timer.startSingleTimer("lifespan",ValidationsTimedOut(10), 10.seconds)
      Behaviors.setup { context => 
        val sharding = ClusterSharding(context.system)
        EventSourcedBehavior[Command, Event, State](
          PersistenceId(TypeKey.name, marketId),
          UninitializedState(Status.empty(marketId)),
          commandHandler = (state, command) => handleCommands(state, command, sharding, context),
          eventHandler = handleEvents
        )}
  }
  }

  def handleCommands(state: State, command: Command, sharding: ClusterSharding, context: ActorContext[Command]): Effect[Event, State] =  {
    (state, command) match {
    case (state: UninitializedState, command: Open) => open(state, command, sharding, context)
    case (state: OpenState, command: CheckMarketOdds) => validateMarket(state, command)
    case (state: OpenState, command: RequestWalletFunds) => validateFunds(state, command)
    case (state: OpenState, command: ValidationsTimedOut) => checkValidations(state, command)
    case (state: OpenState, command: Settle) => settle(state, command, sharding, context)
    case (state: OpenState, command: ManualSettle) => manualSettle(state, command)
    case (state: OpenState, command: Close) => finish(state, command)
    case (_, command: CancelledState) => cancel(state, command)
    case (_, command: ReplyCommand) => reject(state, command)
    case _ => invalid(state, command, context)
  }}

  sealed trait Event
  case class MarketConfirmed(state: OpenState) extends Event
  case class FundsGranted(state: OpenState) extends Event
  case class AllValidationsOk(state: OpenState) extends Event
  case class Opened(betId: String, marketId: String, walletId: String, odds: Int, stake: Int) extends Event
  case class Settled(betId: String) extends Event
  case class Cancelled(betId: String, reason: String) extends Event
  case class Failed(betId: String, reason: String) extends Event
  case object Closed extends Event


  def handleEvents(state: State, event: Event): State = event match {
    case Opened(betId, marketId, walletId, odds, stake) =>
      OpenState(state.status, None, None)
    case MarketConfirmed(state) => 
      state.copy(marketConfirmed = Some(true)) 
    case FundsGranted(state) => 
      state.copy(fundsConfirmed = Some(true)) 
    case AllValidationsOk(state) => 
      state
    case Closed => 
      ClosedState(state.status)
    case Settled(betId) => 
      SettledState(state.status)
    case Cancelled(betId, reason) => 
      CancelledState(state.status)
    case Failed(betId, reason) => 
      FailedState(state.status)
  } 

  sealed trait Response
  case object Accepted extends Response
  case class RequestUnaccepted(reason: String) extends Response
  case class MarketChanged(betId: String, newPrice: Int) extends Response

  def open(state: State, command: Open, sharding: ClusterSharding, context: ActorContext[Command]): ReplyEffect[Opened, State] = { 
    val open = Opened(state.status.betId, command.marketId, command.walletId, command.odds, command.stake)
    Effect.persist(open)
      .thenRun((_:State) => requestMarketStatus(OpenState(state.status), command, sharding, context))
      .thenRun((_:State) => requestFundsReservation(OpenState(state.status), command,  sharding, context))
      .thenReply(command.replyTo)(_ => Accepted)
  }

  def validateMarket(state: OpenState, command: CheckMarketOdds): Effect[Event, State] = {
    if(command.available){
      Effect.persist(MarketConfirmed(state))
    }else {
      Effect.persist(Failed(state.status.betId, "market odds not available"))
    }
  }

  def validateFunds(state: OpenState, command: RequestWalletFunds): Effect[Event, State] = {
    command.response match {
      case Wallet.Accepted => 
        Effect.persist(FundsGranted(state))
      case Wallet.Rejected => 
        Effect.persist(Failed(state.status.betId, "funds not available"))
    }
  }


  //market changes very fast even if our system haven't register the
  //change we need to take this decision quickly. If the Market is not available
  // we fail fast.
  def requestMarketStatus(state: OpenState, command: Open, sharding: ClusterSharding, context: ActorContext[Command]): Unit = {
    val marketRef = sharding.entityRefFor(Market.TypeKey, command.marketId)

    implicit val timeout: Timeout = Timeout(5, SECONDS)
    //Q what's the use of entityRef.ask?? 
    context.ask(marketRef, Market.GetState){
        case Success(Market.CurrentState(marketState)) =>
          if (oddsDoMatch(marketState, state.status.odds)) {
            CheckMarketOdds(true)
          } else { 
            CheckMarketOdds(false)
          }
        case Failure(ex) => 
            context.log.error(ex.getMessage())
            CheckMarketOdds(false)
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


  def requestFundsReservation(state: OpenState, command: Open,  sharding: ClusterSharding, context: ActorContext[Command]): Unit = {
     val walletRef = sharding.entityRefFor(Wallet.TypeKey, command.walletId)
     val walletResponseMapper: ActorRef[Wallet.UpdatedResponse] =
        context.messageAdapter(rsp => RequestWalletFunds(rsp))
     
     walletRef ! Wallet.ReserveFunds(command.stake, walletResponseMapper) 
  }

  def checkValidations(state: OpenState, command: ValidationsTimedOut): Effect[Event, State] = {
    (state.marketConfirmed, state.fundsConfirmed) match {
      case (Some(true), Some(true)) => 
        Effect.persist(AllValidationsOk(state))
      case _ => 
        Effect.persist(Cancelled(state.status.betId, s"validations didn't passed [${state}]"))
  }}
 

  def oddsDoMatch(marketState: Market.State, odds: Int): Boolean = {
    true
  }

  def checkFunds(stake: Int, fundsId: String): Boolean = {
    true
  }

  def isWinner(state: State, marketInfo: Market.State): Boolean = {
    true
  }

  def auxCreateRequest(stake: Int)(replyTo: ActorRef[Wallet.Response]): Wallet.AddFunds =
    Wallet.AddFunds(stake, replyTo) 

  //one way to avoid adding funds twice is asking
  def settle(
      state: State,
      command: Settle, sharding: ClusterSharding, context: ActorContext[Command]): Effect[Event, State] = {
    implicit val timeout = Timeout(10, SECONDS)
    if (isWinner(state, command.marketInfo)) {
       val walletRef = sharding.entityRefFor(Wallet.TypeKey, state.status.walletId)
       context.ask(walletRef, auxCreateRequest(state.status.stake)) {
          case Success(_) => 
             Close(s"stake reimbursed to wallet [$walletRef]")  
          case Failure(ex) => //I rather retry
             context.log.error(s"state NOT reimbursed to wallet [$walletRef]")
             ManualSettle()
        }
    } 
    Effect.none
  }

  def manualSettle(state: State, command: ManualSettle): Effect[Event, State] = 
    Effect.persist(Failed(state.status.betId, s"Reimbursment unsuccessfull. For wallet [${state.status.walletId}]"))

  def finish(state: State, command: Close): Effect[Event, State] = 
     Effect.persist(Closed) 

  def cancel(
      state: State,
      command: Command): Effect[Event, State] = {
    command match {
      case ValidationsTimedOut(time) => Effect.persist(Cancelled(state.status.betId, s"validation in process when life span expired after [$time] seconds"))
    }
  }

  def reject(
      state: State,
      command: ReplyCommand): Effect[Event, State] = {
    Effect.none.thenReply(command.replyTo)(_ => RequestUnaccepted(s"[$command] is not allowed upon the current state [$state]"))
 }

  def invalid(
      state: State,
      command: Command, context: ActorContext[Command]): Effect[Event, State] = {
    context.log.error(s"Implementation error. Unimplemented command [$command] in state [$state]  ")
    Effect.none
 }


}
