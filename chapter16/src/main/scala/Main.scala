// import akka.actor.typed.ActorSystem
// import akka.actor.typed.scaladsl.Behaviors
// import akka.stream.scaladsl.Source

// object Main {

//   def main(args: Array[String]): Unit = {
//     def riskyHandler(elem: Int): Int =
//       100 / elem

//     implicit val system = ActorSystem(Behaviors.empty, "source")

//     Source(-1 to 1)
//       .map(riskyHandler)
//       .run()
//   }
// }
