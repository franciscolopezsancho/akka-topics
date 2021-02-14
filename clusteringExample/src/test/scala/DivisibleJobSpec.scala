package example.cluster

import akka.actor.testkit.typed.scaladsl.{
  LogCapturing,
  ScalaTestWithActorTestKit,
  TestProbe
}
import akka.actor.typed.{ ActorRef, Behavior }
import akka.actor.typed.scaladsl.{ Behaviors, GroupRouter, Routers }
import akka.actor.typed.receptionist.{ Receptionist, ServiceKey }

import scala.util.{ Failure, Success }
import akka.util.Timeout
import scala.concurrent.duration._

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class DivisibleJobSpec
    extends ScalaTestWithActorTestKit
    with AnyWordSpecLike
    with Matchers
    with LogCapturing {

  "The words app" should {
    "register Workers at start up, and subscribe Masters" ignore {
      // val probe = TestProbe[Master.Command]
      // val masterMonitored = Behaviors.monitor(probe.ref, Master())
      // val master = spawn(masterMonitored, "master0")
      // val worker = spawn(Worker(), "worker0")

      // probe.expectMessageType[Master.WorkersRegistered]
    }
    "send work from the master to the workers" in {
      val probe = TestProbe[Master.Command]
      val router = spawn {
        Routers
          .group(Worker.RegistrationKey)
      }

      for (i <- 0 to 10) {
        //with supervision resume
        spawn(Worker(), s"worker-$i")
      }
      val masterMonitored =
        Behaviors.monitor(probe.ref, Master(router))
      val master = spawn(masterMonitored, "master0")
      val worker0 = spawn(Worker(), "worker0")
      val worker1 = spawn(Worker(), "worker1")

      probe.expectMessage(Master.Tick)
      probe.expectMessage(
        Master.CountedWords(
          Map(
            "this" -> 1,
            "a" -> 2,
            "very" -> 1,
            "simulates" -> 1,
            "simple" -> 1,
            "stream" -> 2)))
    }
  }
}
