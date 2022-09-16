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
    val wick = spawn(VIPGuest(), "Mr.Wick")
    "get notified about all the actors each time an actor registers" in {

      spawn(HotelConcierge())
      LoggingTestKit.info("Mr.Wick is in").expect {
        wick ! VIPGuest.EnterHotel
      }
      val guest2 = spawn(VIPGuest(), "Mr.Ious")
      LoggingTestKit.info("Mr.Ious is in").expect {
        LoggingTestKit.info("Mr.Wick is in").expect {
          guest2 ! VIPGuest.EnterHotel
        }
      }
    }
    //what if I do it independently? Stop the actor
    "find that the actor is registered, with basic Find usage" in {
      val testProbe = TestProbe[ActorRef[VIPGuest.Command]]()
      val finder =
        spawn(GuestSearch("Mr.Wick", testProbe.ref), "searcher")
      finder ! GuestSearch.Find
      testProbe.expectMessageType[ActorRef[VIPGuest.Command]]
    }
    //This test would never pass, its oppossite, its negation
    // could be constructed
    "find that no actor is registered, with basic Find usage" ignore {
      val testProbe = TestProbe[ActorRef[VIPGuest.Command]]()
      val finder =
        spawn(GuestSearch("NoOne", testProbe.ref), "searcher")
      LoggingTestKit.info("no one found").expect {
        finder ! GuestSearch.Find
      }
      testProbe.expectMessageType[ActorRef[VIPGuest.Command]]
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
        testKit.stop(wick.ref)
        LoggingTestKit.info("Mr.Wick is in").expect {
          guest ! VIPGuest.EnterHotel
        }
      }
    }

  }

}