package routers

import akka.actor.typed.scaladsl.Behaviors

object HighWayPatrol {
  sealed trait Command
  final case class Violation(plateNumber: String) extends Command
  final case class WithinLimits(plateNumber: String) extends Command

  def apply() = Behaviors.ignore
}
