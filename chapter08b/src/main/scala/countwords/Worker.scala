package example.countwords

import akka.actor.typed.{ ActorRef, Behavior }
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.receptionist.{ Receptionist, ServiceKey }

object Worker {

  val RegistrationKey = ServiceKey[Worker.Command]("Worker")

  sealed trait Command
  case class Process(text: String, replyTo: ActorRef[Master.Event])
      extends Command
      with CborSerializable

  def apply() = Behaviors.setup[Command] { context =>
    context.log.debug(
      s"${context.self} subscribing to $RegistrationKey")
    context.system.receptionist ! Receptionist
      .Register(RegistrationKey, context.self)

    Behaviors.receiveMessage {
      case Process(text, replyTo) =>
        context.log.debug(s"processing $text")
        replyTo ! Master.CountedWords(processTask(text)) //adapter?
        Behaviors.same
    }
  }

  def processTask(text: String): Map[String, Int] = {
    text
      .split("\\W+")
      .foldLeft(Map.empty[String, Int]) { (mapAccumulator, word) =>
        mapAccumulator + (word -> (mapAccumulator.getOrElse(word, 0) + 1))
      }
  }
}
