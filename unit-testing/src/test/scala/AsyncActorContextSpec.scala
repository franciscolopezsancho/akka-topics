// package actorcontext

// import scala.concurrent.duration._

// import org.scalatest.wordspec.AnyWordSpecLike
// import org.scalatest.matchers.should.Matchers

// import akka.actor.testkit.typed.scaladsl.LogCapturing
// import akka.actor.testkit.typed.scaladsl.LoggingTestKit
// import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
// import akka.actor.testkit.typed.scaladsl.TestProbe
// import akka.actor.typed.scaladsl.Behaviors

// class AsyncActorContextSpec
//     extends ScalaTestWithActorTestKit
//     with AnyWordSpecLike
//     with Matchers
//     with LogCapturing {

//   sealed trait Command

//   case object Fail extends Command

//   case object Stop extends Command

//   case object ReceiveTimeout extends Command

//   case class SetTimeout(duration: FiniteDuration) extends Command

//   sealed trait Event

//   case object TimeoutSet extends Event

//   case object GotReceiveTimeout extends Event

//   "set small receive timeout" in {
//     val probe = TestProbe[Event]()
//     val actor = spawn(
//       Behaviors
//         .receivePartial[Command] {
//           case (_, ReceiveTimeout) =>
//             probe.ref ! GotReceiveTimeout
//             Behaviors.same
//           case (context, SetTimeout(duration)) =>
//             context.setReceiveTimeout(duration, ReceiveTimeout)
//             probe.ref ! TimeoutSet
//             Behaviors.same
//         }
//         .decorate)
//     actor ! SetTimeout(1.nano)
//     probe.expectMessage(TimeoutSet)
//     probe.expectMessage(GotReceiveTimeout)
//   }
// }
// "Deferred behavior" must {
//   "must create underlying" in {
//     val probe = TestProbe[Event]("evt")
//     val behv = Behaviors.setup[Command] { _ =>
//       probe.ref ! Started
//       target(probe.ref)
//     }
//     probe.expectNoMessage() // not yet
//     spawn(behv)
//     // it's supposed to be created immediately (not waiting for first message)
//     probe.expectMessage(Started)
//   }

// }
