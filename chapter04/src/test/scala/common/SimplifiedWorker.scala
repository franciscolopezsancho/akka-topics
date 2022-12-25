package common

import akka.actor.typed.scaladsl.Behaviors

object SimplifiedWorker {
  def apply() = Behaviors.ignore[String]
}
