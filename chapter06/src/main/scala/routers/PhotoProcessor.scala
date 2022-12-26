package routers

import akka.actor.typed.Behavior
import akka.actor.typed.receptionist.ServiceKey
import akka.actor.typed.scaladsl.Behaviors

object PhotoProcessor {
  val key = ServiceKey[String]("photo-processor-key")

  def apply(): Behavior[String] = Behaviors.ignore
}
