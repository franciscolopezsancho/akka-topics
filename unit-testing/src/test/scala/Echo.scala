//When the actor gets a meesage from actorX will
// give him back a after 100.millis
// object Echo {

//   sealed trait Command
//   case class Cry(replyTo: ActorRef[T], word: String, volume: Int) extends Signal
//   case class Sound(replyTo: ActorRef[String], word: String, volume: Int) extends Signal
//   case class Word(word: String, volume: Int) extends Signal
//   def apply(): Behavior[Command] = {
//     Behaviors.withTimers { timers =>
//       Behaviors.receivePartial {
//         case Cry(replyTo, word) =>
//           for(i <- 1 to 10){
//              timers.startSingletimer(s"anUnusedKey-$i", Sound(replyTo, word, volume - 1), 100.millis)
//           }
//         case Sound(replyTo, word, volume) =>
// if (volume > 0) reply ! Word(word, volume)
//       }
//     }
//   }
// }
