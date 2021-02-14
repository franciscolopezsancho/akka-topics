package example.cluster

import akka.actor.typed.{ ActorRef, Behavior }
import akka.actor.typed.scaladsl.{ Behaviors, GroupRouter, Routers }
import akka.actor.typed.receptionist.{ Receptionist, ServiceKey }

import scala.util.{ Failure, Success }
import akka.util.Timeout
import scala.concurrent.duration._

object Worker {

  val RegistrationKey = ServiceKey[Worker.Command]("Worker")

  sealed trait Command
  case class Process(text: String, replyTo: ActorRef[Master.Event])
      extends Command
      with CborSerializable

  def apply() = Behaviors.setup[Command] { context =>
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
