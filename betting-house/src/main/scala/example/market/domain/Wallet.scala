package example.betting


import akka.actor.typed.ActorRef
import akka.cluster.sharding.typed.scaladsl.EntityTypeKey

object Wallet {

	 val TypeKey = EntityTypeKey[Command]("wallet")

	 sealed trait Command 
	 case class ReserveFunds(amount: Int, replyTo: ActorRef[Response]) extends Command //have the id on the ActorRef?
	 case class AddFunds(amount: Int, replyTo: ActorRef[Response]) extends Command //have the id on the ActorRef?


	 sealed trait Response
	 case class FundsReserved(amount: Int) extends Response
	 case class FundsAdded(amount: Int) extends Response
}