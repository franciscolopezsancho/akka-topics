import akka.actor.typed.{ ActorRef, Behavior, SupervisorStrategy }
import akka.actor.typed.scaladsl.{ Behaviors }

import akka.actor.testkit.typed.scaladsl.{
  LogCapturing,
  LoggingTestKit,
  ScalaTestWithActorTestKit,
  TestProbe
}

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class CheckingMessage
    extends ScalaTestWithActorTestKit
    with AnyWordSpecLike
    with Matchers
    with LogCapturing {

  "an actor that restarts" should {
    "not reprocess the message that failed" in {

      val errorMessage = "about to fail with: 2"

      def beh(monitor: ActorRef[Int]): Behavior[Int] =
        Behaviors
          .supervise {
            Behaviors.receive[Int] { (context, message) =>
              message match {
                case 2 =>
                  monitor ! 2
                  throw new IllegalArgumentException(2.toString())
                case _ =>
                  Behaviors.same
              }
            }

          }
          .onFailure(SupervisorStrategy.restart)

      val probe = TestProbe[Int]
      val actor = spawn(beh(probe.ref))
      for (i <- 1 to 10) {
        actor.ref ! i
      }
      probe.expectMessage(2)
      probe.expectNoMessage()
    }
  }
}
