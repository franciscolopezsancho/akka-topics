package faulttolerance2

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.SupervisorStrategy
import akka.actor.typed.scaladsl.Behaviors
import faulttolerance2.exception.CorruptedFileException

import java.io.File

object FileWatcher {

  sealed trait Command
  final case class NewFile(file: File, timeAdded: Long)
      extends Command

  def apply(
      directory: String,
      logProcessor: ActorRef[LogProcessor.Command])
      : Behavior[Command] = {
    Behaviors
      .supervise {
        Behaviors.setup[Command] { context =>
          //checks directory is such
          //for each file in directory sends NewFile to self
          Behaviors.receiveMessage[Command] {
            case NewFile(file, _) => ???
            //sends file to log processor
          }
        }
      }
      .onFailure[CorruptedFileException](SupervisorStrategy.resume)

  }
}
