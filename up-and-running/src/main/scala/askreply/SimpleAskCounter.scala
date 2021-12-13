// package state

// import akka.actor.typed.{ ActorSystem, Behavior }
// import akka.actor.typed.scaladsl.Behaviors
// package ask.simple

// import akka.actor.typed.scaladsl.Behaviors
// import akka.actor.typed.{ ActorRef, ActorSystem, Behavior }
// import scala.concurrent.duration.SECONDS
// import akka.util.Timeout
// import scala.util.{ Failure, Success }
// import scala.util.Random

// object SimpleQuestion extends App {

//   val system: ActorSystem[Guardian.Command] =
//     ActorSystem(Guardian(), "example-ask-without-content")
//   system ! Guardian.Start(5)
// }

// object Guardian {

//   sealed trait Command
//   case class Start(increase: Int) extends Command

//   def apply(): Behavior[Command] =
//     Behaviors.setup { context =>
//       val manager: ActorRef[Manager.Command] =
//         context.spawn(Manager(), "manager-1")
//       Behaviors.receiveMessage {
//         case Start(increase) =>
//           manager ! Manager.Delegate(increase)
//           Behaviors.same
//       }
//     }
// }

// object Manager {

//   sealed trait Command
//   case class Delegate(count: Int) extends Command
//   case class Report(description: String) extends Command

//   def apply(): Behavior[Command] =
//     Behaviors.setup { context =>
//       implicit val timeout: Timeout = Timeout(3, SECONDS)
//       val counter = context.spawn(Counter(), s"counter")
//       Behaviors.receiveMessage { message =>
//         message match {
//           case Delegate(files) =>
//             files.map { file =>
//               context.ask(counter, Counter.Increase) {
//                 case Success(Reader.Done) =>
//                   Report(
//                     s"$file has been read by ${worker.path.name}")
//                 case Failure(ex) =>
//                   Report(
//                     s"reading '$file' has failed with [${ex.getMessage()}")
//               }
//             }
//             Behaviors.same
//           case Report(description) =>
//             context.log.info(description)
//             Behaviors.same
//         }
//       }
//     }
// }

// object Counter {

//   sealed trait Command
//   final case object Increase(replyTo: Counter.Response ) extends Command

//   sealed trait Response
//   final case object Accepted extends Response
//   final case object Rejected extends Response

//   def apply(init: Int, max: Int): Behavior[Command] =
//     Behaviors.receive { (context, message) =>
//       message match {
//         case Increase =>
//           val current = init + 1
//           if (current <= max) {
//             replyTo ! Accepted
//             apply(current, max)
//           } else {
//             replyTo ! Rejected
//             Behaviors.same
//           }
//       }
//     }
// }
