import akka.actor.typed.{
  ActorRef,
  Behavior,
  ChildFailed,
  Terminated
}
import akka.actor.typed.scaladsl.{ Behaviors }

object Watcher {

  sealed trait Command
  case class Watch(ref: ActorRef[String]) extends Command

  def apply(
      children: List[ActorRef[String]] = List()): Behavior[Command] =
    Behaviors
      .receive[Command] { (context, message) =>
        message match {
          case Watch(ref) =>
            context.watch(ref)
            Behaviors.same
        }
      }
      .receiveSignal {
        case (context, ChildFailed(ref)) => //unreachable
          context.log.info("childFailed")
          Behaviors.same
        case (context, Terminated(ref)) =>
          context.log.info("terminated")
          Behaviors.same

      }
}
