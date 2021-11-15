package example.betting

import akka.actor.typed.{ ActorRef, Behavior, SupervisorStrategy }
import akka.actor.typed.scaladsl.{
  ActorContext,
  Behaviors,
  TimerScheduler
}
import akka.cluster.sharding.typed.scaladsl.{
  ClusterSharding,
  Entity,
  EntityTypeKey
}
import akka.cluster.sharding.typed.{ ShardingEnvelope }

import akka.persistence.typed.scaladsl.{
  Effect,
  EventSourcedBehavior,
  ReplyEffect,
  RetentionCriteria
}
import akka.persistence.typed.PersistenceId

import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration._
import scala.util.{ Failure, Success }
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
  trait ReplyCommand extends Command with CborSerializable {
    def replyTo: ActorRef[Response]
  }
  case class Open(
      walletId: String,
      marketId: String,
      odds: Double,
      stake: Int,
      result: Int,
      replyTo: ActorRef[Response])
      extends ReplyCommand
  //probably want a local class not to depend on Market? see Market.State below
  case class Settle(result: Int, replyTo: ActorRef[Response])
      extends ReplyCommand
  case class Cancel(reason: String, replyTo: ActorRef[Response])
      extends ReplyCommand
  private case class CheckMarketOdds(available: Boolean)
      extends Command
  private case class RequestWalletFunds(
      response: Wallet.UpdatedResponse)
      extends Command
  private case class ValidationsTimedOut(seconds: Int) extends Command
  private case class Fail(reason: String) extends Command
  private case class Close(reason: String) extends Command

  sealed trait Response
  case object Accepted extends Response
  case object BetVerifying extends Response
  case class RequestUnaccepted(reason: String) extends Response
  case class MarketChanged(betId: String, newPrice: Int)
      extends Response

  //how do I know I bet to the winner or the looser or draw??
  case class Status(
      betId: String,
      walletId: String,
      odds: Double,
      stake: Int,
      result: Int)
      extends CborSerializable
  object Status {
    def empty(marketId: String) = Status(marketId, "", -1, -1, 0)
  }
  sealed trait State extends CborSerializable {
    def status: Status
  }
  case class UninitializedState(status: Status) extends State
  case class OpenState(
      status: Status,
      marketConfirmed: Option[Boolean] = None,
      fundsConfirmed: Option[Boolean] = None)
      extends State // the ask user when market no longer available
  case class SettledState(status: Status) extends State
  case class CancelledState(status: Status) extends State
  case class FailedState(status: Status) extends State
  case class ClosedState(status: Status) extends State

  def apply(betId: String): Behavior[Command] = {
    Behaviors.withTimers { timer =>
      Behaviors
        .setup[Command] { context =>
          val sharding = ClusterSharding(context.system)
          EventSourcedBehavior[Command, Event, State](
            PersistenceId(TypeKey.name, betId),
            UninitializedState(Status.empty(betId)),
            commandHandler = (state, command) =>
              handleCommands(
                state,
                command,
                sharding,
                context,
                timer),
            eventHandler = handleEvents)
        // .withTagger {
        //   case _ => Set(calculateTag(betId, tags))
        // }
        // .withRetention(
        //   RetentionCriteria
        //     .snapshotEvery(
        //       numberOfEvents = 100,
        //       keepNSnapshots = 2))
        // .onPersistFailure(
        //   SupervisorStrategy.restartWithBackoff(
        //     minBackoff = 10.seconds,
        //     maxBackoff = 60.seconds,
        //     randomFactor = 0.1))
        }
    }
  }

  def handleCommands(
      state: State,
      command: Command,
      sharding: ClusterSharding,
      context: ActorContext[Command],
      timer: TimerScheduler[Command]): Effect[Event, State] = {
    (state, command) match {
      case (state: UninitializedState, command: Open) =>
        open(state, command, sharding, context, timer)
      case (state: OpenState, command: CheckMarketOdds) =>
        validateMarket(state, command)
      case (state: OpenState, command: RequestWalletFunds) =>
        validateFunds(state, command)
      case (state: OpenState, command: ValidationsTimedOut) =>
        checkValidations(state, command)
      case (state: OpenState, command: Settle) =>
        settle(state, command, sharding, context)
      case (state: OpenState, command: Close) =>
        finish(state, command)
      case (_, command: Cancel)       => cancel(state, command)
      case (_, command: ReplyCommand) => reject(state, command)
      case (_, command: Fail)         => fail(state, command)
      case _                          => invalid(state, command, context)
    }
  }

  sealed trait Event extends CborSerializable
  case class MarketConfirmed(state: OpenState)
      extends Event
      with CborSerializable
  case class FundsGranted(state: OpenState)
      extends Event
      with CborSerializable
  case class ValidationsPassed(state: OpenState)
      extends Event
      with CborSerializable
  case class Opened(
      betId: String,
      marketId: String,
      walletId: String,
      odds: Double,
      stake: Int,
      result: Int)
      extends Event
      with CborSerializable
  case class Settled(betId: String)
      extends Event
      with CborSerializable
  case class Cancelled(betId: String, reason: String)
      extends Event
      with CborSerializable
  case class Failed(betId: String, reason: String)
      extends Event
      with CborSerializable
  case object Closed extends Event with CborSerializable

  def handleEvents(state: State, event: Event): State = event match {
    case Opened(betId, marketId, walletId, odds, stake, result) =>
      OpenState(state.status, None, None)
    case MarketConfirmed(state) =>
      state.copy(marketConfirmed = Some(true))
    case FundsGranted(state) =>
      state.copy(fundsConfirmed = Some(true))
    case ValidationsPassed(state) =>
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

  def open(
      sta: UninitializedState,
      com: Open,
      sha: ClusterSharding,
      con: ActorContext[Command],
      tim: TimerScheduler[Command]): ReplyEffect[Opened, State] = {
    tim.startSingleTimer(
      "lifespan",
      ValidationsTimedOut(10), // this would read from configuration
      10.seconds)
    val open = Opened(
      sta.status.betId,
      com.marketId,
      com.walletId,
      com.odds,
      com.stake,
      com.result)
    Effect
      .persist(open)
      .thenRun((_: State) =>
        requestMarketStatus(OpenState(sta.status), com, sha, con))
      .thenRun((_: State) =>
        requestFundsReservation(OpenState(sta.status), com, sha, con))
      .thenReply(com.replyTo)(_ => Accepted)
  }

  def validateMarket(
      state: OpenState,
      command: CheckMarketOdds): Effect[Event, State] = {
    if (command.available) {
      Effect.persist(MarketConfirmed(state))
    } else {
      Effect.persist(
        Failed(state.status.betId, "market odds not available"))
    }
  }

  def validateFunds(
      state: OpenState,
      command: RequestWalletFunds): Effect[Event, State] = {
    command.response match {
      case Wallet.Accepted =>
        Effect.persist(FundsGranted(state))
      case Wallet.Rejected =>
        Effect.persist(
          Failed(state.status.betId, "funds not available"))
    }
  }

  //market changes very fast even if our system haven't register the
  //change we need to take this decision quickly. If the Market is not available
  // we fail fast.
  def requestMarketStatus(
      state: OpenState,
      command: Open,
      sharding: ClusterSharding,
      context: ActorContext[Command]): Unit = {
    val marketRef =
      sharding.entityRefFor(Market.TypeKey, command.marketId)

    implicit val timeout: Timeout = Timeout(3, SECONDS)
    //Q what's the use of entityRef.ask?? in the gRPC!
    context.ask(marketRef, Market.GetState) {
      case Success(Market.CurrentState(marketState)) =>
        if (oddsDoMatch(marketState, state.status)) {
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

  def requestFundsReservation(
      state: OpenState,
      command: Open,
      sharding: ClusterSharding,
      context: ActorContext[Command]): Unit = {
    val walletRef =
      sharding.entityRefFor(Wallet.TypeKey, command.walletId)
    val walletResponseMapper: ActorRef[Wallet.UpdatedResponse] =
      context.messageAdapter(rsp => RequestWalletFunds(rsp))

    walletRef ! Wallet.ReserveFunds(
      command.stake,
      walletResponseMapper)
  }

  def checkValidations(
      state: OpenState,
      command: ValidationsTimedOut): Effect[Event, State] = {
    (state.marketConfirmed, state.fundsConfirmed) match {
      case (Some(true), Some(true)) =>
        Effect.persist(ValidationsPassed(state))
      case _ =>
        Effect.persist(
          Cancelled(
            state.status.betId,
            s"validations didn't passed [${state}]"))
    }
  }

  def oddsDoMatch(
      marketStatus: Market.Status,
      betStatus: Bet.Status): Boolean = {
    marketStatus.result match {
      case 0 => marketStatus.odds.draw == betStatus.odds
      case x if x > 0 =>
        marketStatus.odds.winHome == betStatus.odds
      case x if x < 0 =>
        marketStatus.odds.winAway == betStatus.odds
    }
  }

  def checkFunds(stake: Int, fundsId: String): Boolean = {
    true
  }

  def isWinner(state: State, result: Int): Boolean = {
    // marketState.status.result == result
    true
  }

  def auxCreateRequest(stake: Int)(
      replyTo: ActorRef[Wallet.Response]): Wallet.AddFunds =
    Wallet.AddFunds(stake, replyTo)

  //one way to avoid adding funds twice is asking
  def settle(
      state: State,
      command: Settle,
      sharding: ClusterSharding,
      context: ActorContext[Command]): Effect[Event, State] = {
    implicit val timeout = Timeout(10, SECONDS)
    if (isWinner(state, command.result)) {
      val walletRef =
        sharding.entityRefFor(Wallet.TypeKey, state.status.walletId)
      context.ask(walletRef, auxCreateRequest(state.status.stake)) {
        case Success(_) =>
          Close(s"stake reimbursed to wallet [$walletRef]")
        case Failure(ex) => //I rather retry
          val message =
            s"state NOT reimbursed to wallet [$walletRef]. Reason [${ex.getMessage}]"
          context.log.error(message)
          Fail(message)
      }
    }
    Effect.none
  }

  def fail(state: State, command: Command): Effect[Event, State] =
    Effect.persist(Failed(
      state.status.betId,
      s"Reimbursment unsuccessfull. For wallet [${state.status.walletId}]"))

  def finish(state: State, command: Close): Effect[Event, State] =
    Effect.persist(Closed)

  def cancel(state: State, command: Command): Effect[Event, State] = {
    command match {
      case ValidationsTimedOut(time) =>
        Effect.persist(Cancelled(
          state.status.betId,
          s"validation in process when life span expired after [$time] seconds"))
    }
  }

  def reject(
      state: State,
      command: ReplyCommand): Effect[Event, State] = {
    Effect.none.thenReply(command.replyTo)(_ =>
      RequestUnaccepted(
        s"[$command] is not allowed upon the current state [$state]"))
  }

  def invalid(
      state: State,
      command: Command,
      context: ActorContext[Command]): Effect[Event, State] = {
    context.log.error(
      s"Implementation error. Unimplemented command [$command] in state [$state]  ")
    Effect.none
  }

  //TODO read 3 from properties
  val tags = Vector.tabulate(3)(i => s"market-tag-$i")

  def calculateTag(
      entityId: String,
      tags: Vector[String] = tags): String = {
    val tagIndex =
      math.abs(entityId.hashCode % tags.size)
    tags(tagIndex)
  }

}
