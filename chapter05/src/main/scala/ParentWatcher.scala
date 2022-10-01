import akka.actor.typed.{
  ActorRef,
  Behavior,
  ChildFailed,
  Terminated
}
import akka.actor.typed.scaladsl.{ Behaviors }

object ParentWatcher {

  sealed trait Command
  final case class Spawn(behavior: Behavior[String]) extends Command
  final case object StopChildren extends Command
  final case object FailChildren extends Command

  val childBehavior = Behaviors.receivePartial[String] {
    case (_, "stop") =>
      Behaviors.stopped
    case (_, "exception") =>
      throw new Exception()
    case (_, "error") =>
      throw new OutOfMemoryError()
  }

  def apply(
      monitor: ActorRef[String],
      children: List[ActorRef[String]] = List()): Behavior[Command] =
    Behaviors
      .receive[Command] { (context, message) =>
        message match {
          case Spawn(childBehavior) =>
            val child = context.spawnAnonymous(childBehavior)
            context.watch(child)
            apply(monitor, children :+ child)
          case StopChildren =>
            children.map(child => child ! "stop")
            Behaviors.same
          case FailChildren =>
            children.map(child => child ! "exception")
            Behaviors.same
        }
      }
      .receiveSignal {
        case (context, ChildFailed(ref)) =>
          monitor ! "childFailed"
          Behaviors.same
        case (context, Terminated(ref)) =>
          monitor ! "terminated"
          Behaviors.same
      }
}
