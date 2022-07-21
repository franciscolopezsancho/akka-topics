package words

import akka.actor.typed.{ ActorSystem, Behavior }
import akka.actor.typed.scaladsl.Behaviors

object Main extends App {

  val guardian = ActorSystem(Behaviors.empty, "words")
}

