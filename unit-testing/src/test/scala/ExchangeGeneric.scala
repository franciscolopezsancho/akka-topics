// object Exchange[I, O] {

//   sealed trait Command
//   case class Stock(replyTo: ActorRef[?], thing: I)

//   def apply(): Behavior[Command] =
//     Behaviors.receive {
//       case Stock(replyTo, thing) =>
//         replyTo ! new O
//     }
// }
