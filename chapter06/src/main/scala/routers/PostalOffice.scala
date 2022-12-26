package routers

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.Behavior

object PostalOffice {
  sealed trait Command
  final case class Standard(msg: String) extends Command
  final case class Tracked(msg: String) extends Command
  final case class Guaranteed(msg: String) extends Command

  def apply(): Behavior[Command] =
    Behaviors.empty
}
