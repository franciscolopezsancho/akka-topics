package routers

import akka.actor.testkit.typed.scaladsl.{
  LogCapturing,
  ScalaTestWithActorTestKit,
  TestProbe
}

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorRef, Behavior }
import akka.actor.typed.receptionist.Receptionist

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class StateBasedRouterSpec
    extends ScalaTestWithActorTestKit
    with AnyWordSpecLike
    with Matchers
    with LogCapturing {

  "A State Based Router" should {
    val forwardToProbe = TestProbe[String]
    val alertToProbe = TestProbe[String]
    val switch =
      spawn(Switch(forwardToProbe.ref, alertToProbe.ref), "switch")
    "route to forward to actor reference when on" in {
      switch ! Switch.Payload("content1", "metadata1")
      forwardToProbe.expectMessage("content1")
    }
    "route to alert actor and wait when off" in {
      switch ! Switch.SwitchOff
      switch ! Switch.Payload("content2", "metadata2")
      alertToProbe.expectMessage("metadata2")

    }
  }
}
