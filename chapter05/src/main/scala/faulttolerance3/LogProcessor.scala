package faulttolerance3

import akka.actor.typed.Behavior
import akka.actor.typed.SupervisorStrategy
import akka.actor.typed.Terminated
import akka.actor.typed.scaladsl.Behaviors
import faulttolerance3.exception.ParseException

import java.io.File

object LogProcessor {

  sealed trait Command

  final case class LogFile(file: File) extends Command

  def apply(): Behavior[Command] =
    Behaviors
      .supervise {
        Behaviors.setup[Command] { context =>
          // spawn dbWriter with url from settings
          // watch dbWriter
          Behaviors
            .receiveMessage[Command] {
              case LogFile(file) => ???
              //reads file
              //parses by line
              //sends cleaned line to dbWriter
            }
            .receiveSignal {
              case (_, Terminated(ref)) => ???
              //recreate the dbWriter
              //or stop itself
            }
        }
      }
      .onFailure[ParseException](SupervisorStrategy.resume)
}
