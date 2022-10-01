package receptionist

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.ActorRef
import akka.actor.typed.receptionist.{ Receptionist, ServiceKey }
import akka.util.Timeout
import scala.util.{ Failure, Success }
import scala.concurrent.duration._

object GuestFinder {

  sealed trait Command
  final case class Find(
      actorName: String,
      replyTo: ActorRef[ActorRef[VIPGuest.Command]])
      extends Command

  final case object Void extends Command

  def apply() =
    Behaviors.setup[Command] { context =>
      implicit val timeout: Timeout = 3.seconds
      Behaviors.receiveMessage {
        case Find(actorName, replyTo) =>
          context.ask(
            context.system.receptionist,
            Receptionist.Find(HotelConcierge.GoldenKey)) {
            case Success(
                HotelConcierge.GoldenKey.Listing(listings)) =>
              listings
                .filter(_.path.name.contains(actorName))
                .foreach(actor => replyTo ! actor)
              Void

            case Failure(ex) =>
              context.log.error(ex.getMessage())
              Void
          }
          Behaviors.same

        case Void =>
          Behaviors.empty
      }

    }
}
