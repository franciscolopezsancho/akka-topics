// package asyncclock

// import akka.actor.testkit.typed.scaladsl.ActorTestKit
// import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
// import akka.actor.testkit.typed.scaladsl.ManualTime
// import akka.actor.testkit.typed.scaladsl.TestProbe

// import org.scalatest.BeforeAndAfterAll
// import org.scalatest.matchers.should.Matchers
// import org.scalatest.wordspec.AnyWordSpecLike
// import akka.actor.typed.scaladsl.Behaviors
// import akka.actor.typed.{ ActorRef, Behavior }
// import scala.concurrent.duration._

// class AsyncClockSpec
//     extends ScalaTestWithActorTestKit(ManualTime.config)
//     with AnyWordSpecLike
//     with Matchers {

//   val manualTime: ManualTime = ManualTime()

//   "Manual Time" must {

//     // "redirect to Reader the content of the messages received" in {
//     //   val exchanger = spawn(SwapDelayed(), "cookie") // create -> monitor
//     //   val probe = TestProbe[String]("counter")
//     //   exchanger ! SwapDelayed.Take(probe.ref, "stuff")
//     //   manualTime.expectNoMessageFor(99.millis, probe)
//     //   manualTime.timePasses(2.millis)
//     //   probe.expectMessage("stuff".reverse)
//     //   manualTime.expectNoMessageFor(1.seconds, probe)

//     // }

//     "switch" in {
//       val probe = TestProbe[Switch.Command]("switch")
//       probe.ref ! Switch.On
//       probe.expectMessage(Switch.On)
//       manualTime.expectNoMessageFor(9.millis, probe)
//       manualTime.timePasses(11.millis)
//       probe.expectMessage(Switch.Off)
//     }
//   }
// }

// // def tap[T: ClassTag] = new BehaviorInterceptor[T, T] {
// //     override def aroundReceive(context: TypedActorContext[T], message: T, target: ReceiveTarget[T]): Behavior[T] =
// //       target(context, message)
// // }

// object ReversedDelayed {

//   sealed trait Command
//   case class Reverse(from: ActorRef[String], word: String)
//       extends Command
//   sealed trait Response
//   case class Reversed(to: ActorRef[String], word: String) extends Response

//   def apply(): Behavior[Command] =
//     Behaviors.withTimers { timers =>
//       Behaviors.receiveMessage {
//         case Reverse(from, thing) =>
//           timers.startSingleTimer(
//             "unusedKey",
//             Give(from, thing),
//             100.millis)
//           Behaviors.same
//         case Give(to, thing) =>
//           to ! thing.reverse
//           Behaviors.same
//       }
//     }
// }

// object Switch {

//   sealed trait Command
//   case object On extends Command
//   case object Off extends Command

//   def apply(): Behavior[Command] =
//     Behaviors.withTimers { timers =>
//       Behaviors.receive { (context, message) =>
//         message match {
//           case On =>
//             timers.startSingleTimer("inc", Off, 10.millis)
//             Behaviors.same
//           case Off =>
//             timers.startSingleTimer("dec", On, 10.millis)
//             Behaviors.same

//         }
//       }
//     }
// }
