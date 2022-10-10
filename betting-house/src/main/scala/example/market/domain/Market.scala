package example.betting

import akka.actor.typed.{ ActorRef, Behavior, SupervisorStrategy }
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

  val typeKey = EntityTypeKey[Command]("market")

  final case class Fixture(
      id: String,
      homeTeam: String,
      awayTeam: String)
      extends CborSerializable
  final case class Odds(
      winHome: Double,
      winAway: Double,
      draw: Double)
      extends CborSerializable

  sealed trait Command extends CborSerializable {
    def replyTo: ActorRef[Response]
  }
  final case class Open(
      fixture: Fixture,
      odds: Odds,
      opensAt: OffsetDateTime,
      replyTo: ActorRef[Response])
      extends Command

  final case class Update(
      odds: Option[Odds],
      opensAt: Option[OffsetDateTime],
      result: Option[Int], //1 =  winHome, 2 = winAway, 0 = draw
      replyTo: ActorRef[Response])
      extends Command

  final case class Close(replyTo: ActorRef[Response]) extends Command

  final case class Cancel(reason: String, replyTo: ActorRef[Response])
      extends Command
  final case class GetState(replyTo: ActorRef[Response])
      extends Command

  sealed trait Response extends CborSerializable
  final case object Accepted extends Response
  final case class CurrentState(status: Status) extends Response
  final case class RequestUnaccepted(reason: String) extends Response

  sealed trait State extends CborSerializable {
    def status: Status;
  }
  final case class Status(
      marketId: String,
      fixture: Fixture,
      odds: Odds,
      result: Int)
      extends CborSerializable
  object Status {
    def empty(marketId: String) =
      Status(marketId, Fixture("", "", ""), Odds(-1, -1, -1), 0)
  }
  final case class UninitializedState(status: Status) extends State
  final case class OpenState(status: Status) extends State
  final case class ClosedState(status: Status) extends State
  final case class CancelledState(status: Status) extends State

  def apply(marketId: String): Behavior[Command] =
    EventSourcedBehavior[Command, Event, State](
      PersistenceId(typeKey.name, marketId),
      UninitializedState(Status.empty(marketId)),
      commandHandler = handleCommands,
      eventHandler = handleEvents)
      .withTagger {
        case _ => Set(calculateTag(marketId, tags))
      }
      .withRetention(RetentionCriteria
        .snapshotEvery(numberOfEvents = 100, keepNSnapshots = 2))
      .onPersistFailure(
        SupervisorStrategy.restartWithBackoff(
          minBackoff = 10.seconds,
          maxBackoff = 60.seconds,
          randomFactor = 0.1))

  private def handleCommands(
      state: State,
      command: Command): ReplyEffect[Event, State] =
    (state, command) match {
      case (state: UninitializedState, command: Open) =>
        open(state, command)
      case (state: OpenState, command: Update) =>
        update(state, command)
      case (state: OpenState, command: Close) => close(state, command)
      case (_, command: Cancel)               => cancel(state, command)
      case (_, command: GetState)             => tell(state, command)
      case _                                  => invalid(state, command)
    }

  sealed trait Event extends CborSerializable {
    def marketId: String
  }
  final case class Opened(
      marketId: String,
      fixture: Fixture,
      odds: Odds)
      extends Event
  final case class Updated(
      marketId: String,
      odds: Option[Odds],
      result: Option[Int])
      extends Event
  final case class Closed(
      marketId: String,
      result: Int,
      at: OffsetDateTime)
      extends Event
  final case class Cancelled(marketId: String, reason: String)
      extends Event

  private def handleEvents(state: State, event: Event): State = {
    (state, event) match {
      case (_, Opened(marketId, fixture, odds)) =>
        OpenState(Status(marketId, fixture, odds, 0))
      case (state: OpenState, Updated(_, odds, result)) =>
        state.copy(status = Status(
          state.status.marketId,
          state.status.fixture,
          odds.getOrElse(state.status.odds),
          result.getOrElse(state.status.result)))
      case (state: OpenState, Closed(_, result, _)) =>
        ClosedState(state.status.copy(result = result))
      case (_, Cancelled(_, _)) =>
        CancelledState(state.status)
    }
  }

  private def open(
      state: State,
      command: Open): ReplyEffect[Opened, State] = {
    val opened =
      Opened(state.status.marketId, command.fixture, command.odds)
    Effect
      .persist(opened)
      .thenReply(command.replyTo)(_ => Accepted)
  }

  private def update(
      state: State,
      command: Update): ReplyEffect[Updated, State] = {
    val updated =
      Updated(state.status.marketId, command.odds, command.result)
    Effect
      .persist(updated)
      .thenReply(command.replyTo)(_ => Accepted)
  }

  private def close(
      state: State,
      command: Close): ReplyEffect[Closed, State] = {
    val closed = Closed(
      state.status.marketId,
      state.status.result,
      OffsetDateTime.now(ZoneId.of("UTC")))
    Effect
      .persist(closed)
      .thenReply(command.replyTo)(_ => Accepted)
  }

  private def cancel(
      state: State,
      command: Cancel): ReplyEffect[Cancelled, State] = {
    val cancelled = Cancelled(state.status.marketId, command.reason)
    Effect
      .persist(cancelled)
      .thenReply(command.replyTo)((_: State) => Accepted)
  }

  private def tell(
      state: State,
      command: GetState): ReplyEffect[Event, State] = {
    Effect.none.thenReply(command.replyTo)(_ =>
      CurrentState(state.status))
  }

  private def invalid(
      state: State,
      command: Command): ReplyEffect[Event, State] = {
    Effect.none.thenReply(command.replyTo)(
      _ =>
        RequestUnaccepted(
          s"[$command] is not allowed upon state [$state]"))
  }

  //TODO read 3 from properties
  val tags = Vector.tabulate(3)(i => s"market-tag-$i")

  private def calculateTag(
      entityId: String,
      tags: Vector[String] = tags): String = {
    val tagIndex =
      math.abs(entityId.hashCode % tags.size)
    tags(tagIndex)
  }

}
