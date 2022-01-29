package state

import akka.actor.typed.{ ActorSystem, Behavior }
import akka.actor.typed.scaladsl.Behaviors

object Counter {

  sealed trait Command
  final case object Increase extends Command

  def apply(count: Int, max: Int): Behavior[Command] =
    Behaviors.receive { (context, message) =>
      message match {
        case Increase =>
          val current = count + 1
          if (current <= max) {
            context.log.info(s"increasing to $current")
            apply(current, max)
          } else {
            context.log.info(
              s"I'm overloaded. Counting '$current' while max is '$max")
            Behaviors.stopped
          }
      }
    }
}

object CounterApp extends App {

  val guardian: ActorSystem[Counter.Command] =
    ActorSystem(Counter(0, 2), "counter")
  guardian ! Counter.Increase
  guardian ! Counter.Increase
  guardian ! Counter.Increase

}
