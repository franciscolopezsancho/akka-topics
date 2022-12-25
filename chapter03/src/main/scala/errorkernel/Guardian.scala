package errorkernel

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

object Guardian {

  sealed trait Command
  final case class Start(texts: List[String]) extends Command

  def apply(): Behavior[Command] =
    Behaviors.setup { context =>
      context.log.info("Setting up. Creating manager")
      val manager: ActorRef[Manager.Command] =
        context.spawn(Manager(), "manager-alpha")
      Behaviors.receiveMessage {
        case Start(texts) =>
          manager ! Manager.Delegate(texts)
          Behaviors.same
      }
    }
}
