package receptionist

import akka.actor.testkit.typed.scaladsl.{
  LogCapturing,
  LoggingTestKit,
  ScalaTestWithActorTestKit,
  TestProbe
}

import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.matchers.should.Matchers

import akka.actor.typed.ActorRef

class ReceptionistUsageSpec
    extends ScalaTestWithActorTestKit
    with AnyWordSpecLike
    with Matchers
    with LogCapturing {

  "An actor subscribed to a ServiceKey " should {
    val guest = spawn(VIPGuest(), "Mr.Wick")
    "get notified about all the actors each time an actor registers" in {

      spawn(HotelConcierge())
      LoggingTestKit.info("Mr.Wick is in").expect {
        guest ! VIPGuest.EnterHotel
      }
      val guest2 = spawn(VIPGuest(), "Mr.Ious")
      LoggingTestKit.info("Mr.Ious is in").expect {
        LoggingTestKit.info("Mr.Wick is in").expect {
          guest2 ! VIPGuest.EnterHotel
        }
      }
    }

    "find that the actor is registered, with basic Find usage" in {
      val testProbe = TestProbe[ActorRef[VIPGuest.Command]]()
      val finder =
        spawn(GuestSearch("Mr.Wick", testProbe.ref), "searcher1")
      finder ! GuestSearch.Find
      testProbe.expectMessageType[ActorRef[VIPGuest.Command]]
    }

    "find that no actor is registered, with basic Find usage" in {
      val testProbe = TestProbe[ActorRef[VIPGuest.Command]]()
      val finder =
        spawn(GuestSearch("NoOne", testProbe.ref), "searcher2")
      finder ! GuestSearch.Find
      testProbe.expectNoMessage
    }

    "find that the actor is registered, with search params in Find" in {
      val probe = TestProbe[ActorRef[VIPGuest.Command]]()
      val finder = spawn(GuestFinder(), "finder")
      finder ! GuestFinder.Find("Mr.Wick", probe.ref)
      probe.expectMessageType[ActorRef[VIPGuest.Command]]
    }

    "get notified only alive actors" in {
      intercept[AssertionError] {
        val guest = spawn(VIPGuest(), "Mrs.X")
        testKit.stop(guest.ref)
        LoggingTestKit.info("Mr.Wick is in").expect {
          guest ! VIPGuest.EnterHotel
        }
      }
    }

  }

}
