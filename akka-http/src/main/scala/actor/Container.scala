// package example.actor

// import akka.actor.typed.{ ActorRef, Behavior }
// import akka.actor.typed.scaladsl.Behaviors

// object Container {

//   case class Cargo(kind: String, size: Int)

//   sealed trait Command
//   case class AddCargo(cargo: Cargo, replyTo: ActorRef[Response])
//       extends Command
//   case class GetCargos(replyTo: ActorRef[List[Cargo]]) extends Command

//   sealed trait Response
//   case object OK extends Response

//   def apply(cargos: List[Cargo] = Nil): Behavior[Command] = {
//     Behaviors.receive { (context, message) =>
//       message match {
//         case AddCargo(cargo, replyTo) =>
//           println(s"adding cargo $cargo")
//           replyTo ! OK
//           apply(cargo +: cargos)
//         case GetCargos(replyTo) =>
//           replyTo ! cargos
//           Behaviors.same
//       }
//     }
//   }
// }
