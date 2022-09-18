package words

import akka.actor.typed.{ ActorSystem, Behavior }
import akka.actor.typed.scaladsl.Behaviors

object Main extends App {

  ActorSystem(Behaviors.empty, "words")
}
