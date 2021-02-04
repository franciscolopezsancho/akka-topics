import akka.actor.typed.{ Behavior, PostStop }
import akka.actor.typed.scaladsl.{ ActorContext, Behaviors }

object SupervisionExample {

  def cleaning(context: ActorContext[String], info: String): Unit =
    context.log.info(info)

  def apply(): Behavior[String] =
    Behaviors
      .receivePartial[String] {
        case (context, "secret") =>
          context.log.info("granted")
          Behaviors.same
        case (context, "stop") =>
          context.log.info("stopping")
          Behaviors.stopped
        case (context, "recoverable") =>
          context.log.info("recoverable")
          throw new IllegalStateException()
          Behaviors.same
        case (context, "fatal") =>
          throw new OutOfMemoryError()
          Behaviors.same
      }
      .receiveSignal {
        case (context, PostStop) =>
          cleaning(context, "cleaning resources")
          Behaviors.same
      }
}
