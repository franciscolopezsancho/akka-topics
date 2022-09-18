package receptionist

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.receptionist.{ Receptionist, ServiceKey }

object HotelConcierge {

  val GoldenKey = ServiceKey[VIPGuest.Command]("concierge-key")

  sealed trait Command
  private case class ListingResponse(listing: Receptionist.Listing)
      extends Command

  def apply() = Behaviors.setup[Command] { context =>
    val listingNotificationAdapter =
      context.messageAdapter[Receptionist.Listing](ListingResponse)

    context.system.receptionist ! Receptionist
      .Subscribe(GoldenKey, listingNotificationAdapter)

    Behaviors.receiveMessage {
      case ListingResponse(GoldenKey.Listing(listings)) =>
        listings.foreach { actor =>
          context.log.info(s"${actor.path.name} is in")
        }
        Behaviors.same
    }
  }
}
