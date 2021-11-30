package example.betting

import akka.actor.typed.{ ActorRef, Behavior, SupervisorStrategy }
import akka.cluster.sharding.typed.scaladsl.EntityTypeKey

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

object Wallet {

  val TypeKey = EntityTypeKey[Command]("wallet")

  sealed trait Command extends CborSerializable
  case class ReserveFunds(
      amount: Int,
      replyTo: ActorRef[UpdatedResponse])
      extends Command
  case class AddFunds(amount: Int, replyTo: ActorRef[UpdatedResponse])
      extends Command
  case class CheckFunds(replyTo: ActorRef[Response]) extends Command

  sealed trait Event extends CborSerializable
  case class FundsReserved(amount: Int) extends Event
  case class FundsAdded(amount: Int) extends Event
  case class FundsReservationDenied(amount: Int) extends Event

  sealed trait Response extends CborSerializable
  trait UpdatedResponse extends Response
  case object Accepted extends UpdatedResponse
  case object Rejected extends UpdatedResponse
  case class CurrentBalance(amount: Int) extends Response

  case class State(balance: Int) extends CborSerializable

  def apply(walletId: String): Behavior[Command] =
    EventSourcedBehavior[Command, Event, State](
      PersistenceId(TypeKey.name, walletId),
      State(0),
      commandHandler = handleCommands,
      eventHandler = handleEvents)
      .withTagger {
        case _ => Set(calculateTag(walletId, tags))
      }
      .withRetention(RetentionCriteria
        .snapshotEvery(numberOfEvents = 100, keepNSnapshots = 2))
      .onPersistFailure(
        SupervisorStrategy.restartWithBackoff(
          minBackoff = 10.seconds,
          maxBackoff = 60.seconds,
          randomFactor = 0.1))

  def handleCommands(
      state: State,
      command: Command): ReplyEffect[Event, State] = {
    command match {
      case ReserveFunds(amount, replyTo) =>
        //it might need to check with an external service to
        // prove the customer is not betting such it can be considered addiction.
        if (amount <= state.balance)
          Effect
            .persist(FundsReserved(amount))
            .thenReply(replyTo)(state => Accepted)
        else
          Effect
            .persist(FundsReservationDenied(amount))
            .thenReply(replyTo)(state => Rejected)
      case AddFunds(amount, replyTo) =>
        Effect
          .persist(FundsAdded(amount))
          .thenReply(replyTo)(state => Accepted)
      case CheckFunds(replyTo) =>
        Effect.reply(replyTo)(CurrentBalance(state.balance))
    }
  }

  def handleEvents(state: State, event: Event): State = event match {
    case FundsReserved(amount) =>
      State(state.balance - amount)
    case FundsAdded(amount) =>
      State(state.balance + amount)
    case FundsReservationDenied(_) =>
      state
  }

  val tags = Vector.tabulate(3)(i => s"wallet-tag-$i")

  private def calculateTag(
      entityId: String,
      tags: Vector[String] = tags): String = {
    val tagIndex =
      math.abs(entityId.hashCode % tags.size)
    tags(tagIndex)
  }
}
