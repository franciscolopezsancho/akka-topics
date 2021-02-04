package receptionist

import akka.actor.testkit.typed.scaladsl.{
  LogCapturing,
  LoggingTestKit,
  ScalaTestWithActorTestKit,
  TestProbe
}

import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.matchers.should.Matchers

import org.slf4j.event.Level

import akka.actor.typed.receptionist.{ Receptionist, ServiceKey }
import akka.actor.typed.ActorRef

class ReceptionistUsageSpec
    extends ScalaTestWithActorTestKit
    with AnyWordSpecLike
    with Matchers
    with LogCapturing {

  "An actor subscribed to a ServiceKey " should {
    val wick = spawn(Guest(), "Mr.Wick")
    "get notified about all the actors each time an actor registers" in {

      spawn(HotelDesk())
      LoggingTestKit.info("Mr.Wick is in").expect {
        wick ! Guest.EnterHotel
      }
      val guest2 = spawn(Guest(), "Mr.Ious")
      LoggingTestKit.info("Mr.Ious is in").expect {
        LoggingTestKit.info("Mr.Wick is in").expect {
          guest2 ! Guest.EnterHotel
        }
      }
    }
    //what if I do it independently? Stop the actor
    "find that the actor is registered, with basic Find usage" in {
      val testProbe = TestProbe[ActorRef[Guest.Command]]()
      val finder =
        spawn(GuestSearch("Mr.Wick", testProbe.ref), "searcher")
      finder ! GuestSearch.Find
      testProbe.expectMessageType[ActorRef[Guest.Command]]
    }
    //This test would never pass, its oppossite, its negation
    // could be constructed
    "find that no actor is registered, with basic Find usage" ignore {
      val testProbe = TestProbe[ActorRef[Guest.Command]]()
      val finder =
        spawn(GuestSearch("NoOne", testProbe.ref), "searcher")
      LoggingTestKit.info("no one found").expect {
        finder ! GuestSearch.Find
      }
      testProbe.expectMessageType[ActorRef[Guest.Command]]
    }
    "find that the actor is registered, with search params in Find" in {
      val probe = TestProbe[ActorRef[Guest.Command]]()
      val finder = spawn(GuestFinder(), "finder")
      finder ! GuestFinder.Find("Mr.Wick", probe.ref)
      probe.expectMessageType[ActorRef[Guest.Command]]
    }
    "get notified only alive actors" in {
      intercept[AssertionError] {
        val guest = spawn(Guest(), "Mrs.X")
        testKit.stop(wick.ref)
        LoggingTestKit.info("Mr.Wick is in").expect {
          guest ! Guest.EnterHotel
        }
      }
    }

  }

}

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.ActorRef
import akka.actor.typed.receptionist.{ Receptionist, ServiceKey }
import akka.util.Timeout
import scala.util.{ Failure, Success }
import scala.concurrent.duration._

object Guest {

  sealed trait Command
  case object EnterHotel extends Command
  case object LeaveHotel extends Command

  def apply() = Behaviors.receive[Command] { (context, message) =>
    message match {
      case EnterHotel =>
        context.system.receptionist ! Receptionist
          .Register(HotelDesk.GoldenKey, context.self)
        Behaviors.same

      case LeaveHotel =>
        context.system.receptionist ! Receptionist
          .Deregister(HotelDesk.GoldenKey, context.self)
        Behaviors.same
    }

  }
}

object HotelDesk {

  val GoldenKey = ServiceKey[Guest.Command]("hotel-key")

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

// object BadBehavior {

//   sealed trait Command
//   case object RemoveGuest extends Command

//   private case class ListingResponse(listing: Receptionist.Listing)
//       extends Command

//   def apply(actorName: String) = Behaviors.setup[Command] { context =>

//     val listingResponseAdapter =
//       context.messageAdapter[Receptionist.Listing](ListingResponse)

//     Behaviors.receiveMessage {
//       case RemoveGuest =>
//         context.system.receptionist ! Receptionist
//           .Find(HotelDesk.GoldenKey, listingResponseAdapter)
//         Behaviors.same
//       case ListingResponse(HotelDesk.GoldenKey.Listing(listings)) =>
//         listings
//           .filter(_.path.name.contains(actorName))
//           .foreach(actor => actor ! Guest.LeaveHotel)
//         Behaviors.same
//     }
//   }
// }

//here you can NOT pass the actor to look for and to answer to in the message
object GuestSearch {

  sealed trait Command
  case object Find extends Command
  private case class ListingResponse(listings: Receptionist.Listing)
      extends Command

  def apply(
      actorName: String,
      replyTo: ActorRef[ActorRef[Guest.Command]]) =
    Behaviors.setup[Command] { context =>

      val listingResponseAdapter =
        context.messageAdapter[Receptionist.Listing](ListingResponse)

      Behaviors.receiveMessage {
        case Find =>
          context.system.receptionist ! Receptionist
            .Find(HotelDesk.GoldenKey, listingResponseAdapter)
          Behaviors.same

        case ListingResponse(HotelDesk.GoldenKey.Listing(listings)) =>
          listings
            .filter(_.path.name.contains(actorName))
            .foreach(actor => replyTo ! actor)
          Behaviors.stopped
      }
    }
}

//here you can pass the actor to look for and to answer to in the message
object GuestFinder {

  sealed trait Command
  case class Find(
      actorName: String,
      replyTo: ActorRef[ActorRef[Guest.Command]])
      extends Command

  case object Void extends Command

  def apply() =
    Behaviors.setup[Command] { context =>
      implicit val timeout: Timeout = 3.seconds
      Behaviors.receiveMessage {
        case Find(actorName, replyTo) =>
          context.ask(
            context.system.receptionist,
            Receptionist.Find(HotelDesk.GoldenKey)) {
            case Success(HotelDesk.GoldenKey.Listing(listings)) =>
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
