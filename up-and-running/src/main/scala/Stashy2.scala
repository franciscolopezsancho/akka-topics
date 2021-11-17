// package stash

// object Main extends App {}

// object Guardian {

//   def apply(): Behavior[Unit] = {
//     Behaviors.setup[Unit] { context =>
//       val process = context.spawnAnonymous(Processor())
//     }

//   }
// }

// object Processor {

//   def apply(): Behavior[String] = {
//     Behaviors.withStash(1000) { //how much is StashBuffer size? let's put max

//     }
//   }
//   //how does it work if I stash a message from the mailbox? do
//   //they get always keep in front when unstash?

//   def upAndRunning(): Behavior[String] = {}

//   def configuring(): Behavior[String] = {}
// }

// trait ExternalService {
//   def retrieveConfig(id: String): Future[Config]
// }
