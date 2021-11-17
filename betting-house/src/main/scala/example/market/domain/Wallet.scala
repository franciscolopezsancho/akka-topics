package example.betting

import akka.actor.typed.{ ActorRef, Behavior }
import akka.cluster.sharding.typed.scaladsl.EntityTypeKey

import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.sharding.typed.scaladsl.EntityTypeKey

import akka.persistence.typed.scaladsl.{
  Effect,
  EventSourcedBehavior,
  ReplyEffect
}
import akka.persistence.typed.PersistenceId

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
}
