package example.betting

import akka.actor.typed.{ ActorRef, Behavior, SupervisorStrategy }
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.sharding.typed.scaladsl.EntityTypeKey

import akka.persistence.typed.scaladsl.{
  Effect,
  EventSourcedBehavior,
  ReplyEffect,
  RetentionCriteria
}
import akka.persistence.typed.PersistenceId

import scala.concurrent.duration._
import java.time.{ OffsetDateTime, ZoneId }

/**
 *
 */
object Market {

  val TypeKey = EntityTypeKey[Command]("market")

  case class Fixture(id: String, homeTeam: String, awayTeam: String)
      extends CborSerializable
  case class Odds(winHome: Double, winAway: Double, draw: Double)
      extends CborSerializable //Q - its total sum must be 2?

  sealed trait Command extends CborSerializable {
    def replyTo: ActorRef[Response]
  }
  case class Initialize(
      fixture: Fixture,
      odds: Odds,
      opensAt: OffsetDateTime,
      replyTo: ActorRef[Response])
      extends Command

  case class Update(
      odds: Odds,
      opensAt: OffsetDateTime,
      result: Int, //+ winHome, - winAway, 0 draw
      replyTo: ActorRef[Response])
      extends Command

  case class Open(replyTo: ActorRef[Response]) extends Command

  case class Close(replyTo: ActorRef[Response]) extends Command

  case class Cancel(reason: String, replyTo: ActorRef[Response])
      extends Command
  case class GetState(replyTo: ActorRef[Response]) extends Command

  sealed trait Response extends CborSerializable
  case object Accepted extends Response
  case class CurrentState(status: Status) extends Response
  case class RequestUnaccepted(reason: String) extends Response

  sealed trait State extends CborSerializable {
    def status: Status;
  }
  case class Status(
      marketId: String,
      fixture: Fixture,
      odds: Odds,
      result: Int)
      extends CborSerializable
  object Status {
    def empty(marketId: String) =
      Status(marketId, Fixture("", "", ""), Odds(-1, -1, -1), 0)
  }
  case class Uninitialized(status: Status) extends State

  case class InitializedState(status: Status) extends State

  case class OpenState(status: Status) extends State

  case class ClosedState(status: Status) extends State

  case class CancelledState(status: Status) extends State

  def apply(marketId: String): Behavior[Command] =
    EventSourcedBehavior[Command, Event, State](
      PersistenceId(TypeKey.name, marketId),
      Uninitialized(Status.empty(marketId)),
      commandHandler = handleCommands,
      eventHandler = handleEvents)
    // .withTagger {
    //   case _ => Set(calculateTag(marketId, tags))
    // }
      .withRetention(RetentionCriteria
        .snapshotEvery(numberOfEvents = 100, keepNSnapshots = 2))
      .onPersistFailure(
        SupervisorStrategy.restartWithBackoff(
          minBackoff = 10.seconds,
          maxBackoff = 60.seconds,
          randomFactor = 0.1))

  def handleCommands(
      state: State,
      command: Command): ReplyEffect[Event, State] =
    (state, command) match {
      case (state: Uninitialized, command: Initialize) =>
        init(state, command)
      case (state: InitializedState, command: Update) =>
        update(state, command)
      case (state: InitializedState, command: Open) =>
        open(state, command)
      case (state: OpenState, command: Update) =>
        update(state, command)
      // case (state: SuspendedState, command: Update) =>
      //   update(state, command)
      // case (state: SuspendedState, command: Open) =>
      //  open(state, command)
      case (state: OpenState, command: Close) => close(state, command)
      case (_, command: Cancel)               => cancel(state, command)
      case (_, command: GetState)             => tell(state, command)
      case _                                  => invalid(state, command)
    }

  sealed trait Event extends CborSerializable
  case class Initialized(
      marketId: String,
      fixture: Fixture,
      odds: Odds)
      extends Event

  case class Opened(marketId: String) extends Event

  case class Updated(marketId: String, odds: Odds, result: Int)
      extends Event

  case class Closed(marketId: String, at: OffsetDateTime)
      extends Event

  case class Cancelled(marketId: String, reason: String) extends Event

  def handleEvents(state: State, event: Event): State = {
    (state, event) match {
      case (_, Initialized(marketId, fixture, odds)) =>
        InitializedState(Status(marketId, fixture, odds, 0))
      case (_, Opened(marketId)) =>
        OpenState(state.status)
      case (state: OpenState, Updated(_, odds, result)) =>
        state.copy(status = Status(
          state.status.marketId,
          state.status.fixture,
          odds,
          result))
      case (_, Closed(marketId, _)) =>
        ClosedState(state.status)
      case (_, Cancelled(marketId, reason)) =>
        CancelledState(state.status)
    }
  }

  def init(
      state: State,
      command: Initialize): ReplyEffect[Initialized, State] = {
    val initialized = Initialized(
      state.status.marketId,
      command.fixture,
      command.odds)
    Effect
      .persist(initialized)
      .thenReply(command.replyTo)(_ => Accepted)
  }

  def update(
      state: State,
      command: Update): ReplyEffect[Updated, State] = {
    val updated =
      Updated(state.status.marketId, command.odds, command.result)
    Effect
      .persist(updated)
      .thenReply(command.replyTo)(_ => Accepted)
  }

  def open(
      state: State,
      command: Open): ReplyEffect[Opened, State] = {
    Effect
      .persist(Opened(state.status.marketId))
      .thenReply(command.replyTo)(_ => Accepted)
  }

  def close(
      state: State,
      command: Close): ReplyEffect[Closed, State] = {
    val closed = Closed(
      state.status.marketId,
      OffsetDateTime.now(ZoneId.of("UTC")))
    Effect
      .persist(closed)
      .thenReply(command.replyTo)(_ => Accepted)
  }

  def cancel(
      state: State,
      command: Cancel): ReplyEffect[Cancelled, State] = {
    val cancelled = Cancelled(state.status.marketId, command.reason)
    Effect
      .persist(cancelled)
      .thenReply(command.replyTo)((_: State) => Accepted)
  }

  def tell(
      state: State,
      command: GetState): ReplyEffect[Event, State] = {
    Effect.none.thenReply(command.replyTo)(_ =>
      CurrentState(state.status))
  }

  def invalid(
      state: State,
      command: Command): ReplyEffect[Event, State] = {
    Effect.none.thenReply(command.replyTo)(
      _ =>
        RequestUnaccepted(
          s"[$command] is not allowed upon state [$state]"))
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
