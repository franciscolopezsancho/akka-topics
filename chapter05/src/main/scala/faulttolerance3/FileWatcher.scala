package faulttolerance3

import akka.actor.typed.Behavior
import akka.actor.typed.SupervisorStrategy
import akka.actor.typed.Terminated
import akka.actor.typed.scaladsl.Behaviors
import faulttolerance3.exception.ClosedWatchServiceException

import java.io.File

object FileWatcher extends FileListeningAbilities {

  sealed trait Command

  final case class NewFile(file: File, timeAdded: Long)
      extends Command

  final case class FileModified(file: File, timeAdded: Long)
      extends Command

  def apply(directory: String): Behavior[Command] =
    Behaviors
      .supervise {
        Behaviors.setup[Command] { context =>
          //starts listening to directory, spawns and watches a LogProcessor
          //when new file in the directory it receives NewFile
          //when modified file in the directory it receives FileModified
          Behaviors
            .receiveMessage[Command] {
              case NewFile(file, _) => ???
              //sends file to log processor
              case FileModified(file, _) => ???
              //send event to log processor
            }
            .receiveSignal {
              case (_, Terminated(ref)) => ???
              // stop itself as LogProcessor will be unresumable
            }
        }
      }
      .onFailure[ClosedWatchServiceException](
        SupervisorStrategy.restart)
  //after restart work can get duplicated, deduplication is out of the scope.

}
