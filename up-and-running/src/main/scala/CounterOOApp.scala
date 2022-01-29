package oo

import akka.actor.typed.{ ActorSystem, Behavior }
import akka.actor.typed.scaladsl.{
  AbstractBehavior,
  ActorContext,
  Behaviors
}

object Counter {

  sealed trait Command
  final case object Increase extends Command

  def apply(init: Int, max: Int): Behavior[Command] =
    Behaviors.setup((context) => new Counter(init, max, context))

  class Counter(init: Int, max: Int, context: ActorContext[Command])
      extends AbstractBehavior[Command](context) {
    var current = init

    override def onMessage(message: Command): Behavior[Command] =
      message match {
        case Increase =>
          current += 1
          context.log.info(s"increasing to $current")
          if (current > max) {
            context.log.info(
              s"I'm overloaded. Counting '$current' while max is '$max")
            Behaviors.stopped
          } else {
            this
          }
      }
  }
}

object CounterOOApp extends App {

  val guardian: ActorSystem[Counter.Command] =
    ActorSystem(Counter(0, 2), "counter")
  guardian ! Counter.Increase
  guardian ! Counter.Increase
  guardian ! Counter.Increase

}
