package common

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.Behaviors
import scala.concurrent.duration._

object SimplifiedManager {

  sealed trait Command
  final case class CreateChild(name: String) extends Command
  final case class Forward(message: String, sendTo: ActorRef[String])
      extends Command
  final case object ScheduleLog extends Command
  final case object Log extends Command

  def apply(): Behaviors.Receive[Command] =
    Behaviors.receive { (context, message) =>
      message match {
        case CreateChild(name) =>
          context.spawn(SimplifiedWorker(), name)
          Behaviors.same
        case Forward(text, sendTo) =>
          sendTo ! text
          Behaviors.same
        case ScheduleLog =>
          context.scheduleOnce(1.seconds, context.self, Log)
          Behaviors.same
        case Log =>
          context.log.info(s"it's done")
          Behaviors.same
      }
    }
}


