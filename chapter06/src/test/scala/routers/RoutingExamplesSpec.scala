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

class RoutingExamplesSpec
    extends ScalaTestWithActorTestKit
    with AnyWordSpecLike
    with Matchers
    with LogCapturing {

  "a pool router" should {
    "send messages in round-robing fashion" in {

      val probe = TestProbe[String]
      val worker = Worker(probe.ref)
      val router = spawn(Manager(worker), "round-robin")

      probe.expectMessage("hi")
      probe.receiveMessages(10)
    }

    "Broadcast, sending each message to all routtes" in {

      val probe = TestProbe[String]
      val worker = Worker(probe.ref)

      val router = spawn(BroadcastingManager(worker), "broadcasting")

      probe.expectMessage("hi, there")
      probe.receiveMessages(43)

    }

    "ConstantHashing send messages to only one" ignore {

      val probe = TestProbe[String]
      val worker = Worker(probe.ref)

      val router = spawn(Manager(worker), "constant-hashing")

      probe.expectMessage("hi")
      probe.receiveMessages(10)

    }
  }
  "a group router" should {
    "send messages to one worker registered at a key" ignore { // because the same key conflict with
      // next test "send messages to all worker registered at a key"
      val probe1 = TestProbe[String]
      val behavior1 =
        Behaviors.monitor(probe1.ref, Behaviors.empty[String])

      system.receptionist ! Receptionist.Register(
        PhotoProcessor.Key,
        spawn(behavior1))

      val groupRouter = spawn(Camera())
      groupRouter ! Camera.Photo("hi")

      probe1.expectMessage("hi")
      probe1.receiveMessages(1)

    }

    //TODO review this test may eventually fail. 
    // becuase one photoProcessor takes all the messages.
    "send messages to all photo processors registered" in {
      val photoProcessor1 = TestProbe[String]
      val pp1Monitor = Behaviors.monitor(photoProcessor1.ref, PhotoProcessor())

      val photoProcessor2 = TestProbe[String]
      val pp2Monitor = Behaviors.monitor(photoProcessor2.ref, PhotoProcessor())

      system.receptionist ! Receptionist.Register(
        PhotoProcessor.Key,
        spawn(pp1Monitor))
      system.receptionist ! Receptionist.Register(
        PhotoProcessor.Key,
        spawn(pp2Monitor))

      val camera = spawn(Camera())
      camera ! Camera.Photo("A")
      camera ! Camera.Photo("B")

      photoProcessor1.receiveMessages(1)

      photoProcessor2.receiveMessages(1)
    }

    "will send messages with same id to the same aggregator" in {
      //Given there are two Aggregators
      val probe1 = TestProbe[Aggregator.Event]
      val probe2 = TestProbe[Aggregator.Event]

      spawn(Aggregator(forwardTo = probe1.ref), "aggregator1")
      spawn(Aggregator(forwardTo = probe2.ref), "aggregator2")

      val contentValidator = spawn(DataObfuscator(), "wa-1")
      val dataEnricher = spawn(DataEnricher(), "wb-1")

      //When a message with same id is sent to different actors
      contentValidator ! DataObfuscator.Message("123", "Text")
      dataEnricher ! DataEnricher.Message("123", "Text")
      //Then one aggregator receives both while the other receives none
      probe1.expectMessage(
        Aggregator.Completed("123", "text", "metadata"))
      probe2.expectNoMessage()

      //When a message with same id is sent to different actors
      contentValidator ! DataObfuscator.Message("z23", "LoreIpsum")
      dataEnricher ! DataEnricher.Message("z23", "LoreIpsum")
      //Then one aggregator receives both while the other receives none
      probe1.expectNoMessage()
      probe2.expectMessage(
        Aggregator.Completed("z23", "loreipsum", "metadata"))
    }
  }

  "A State Router" should {
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