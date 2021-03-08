package aia.faulttolerance3

import java.io.File
import akka.actor.typed.scaladsl.{ Behaviors }
import akka.actor.typed.{
  ActorRef,
  ActorSystem,
  Behavior,
  PostStop,
  PreRestart,
  SupervisorStrategy,
  Terminated
}

import scala.concurrent.duration._

object LogProcessingApp extends App {
  //this directories may come from settings
  //or from args using Main instead of extending from 'App'
  val directories = Vector("file:///source1/", "file:///source2/")

  val guardian = ActorSystem[Nothing](
    LogProcessingGuardian(directories),
    "log-processing-app")
}

object LogProcessingGuardian {

  def apply(directories: Vector[String]) =
    Behaviors
      .setup[Nothing] { context =>
        directories.foreach { directory =>

          val fileWatcher: ActorRef[FileWatcher.Command] =
            context.spawnAnonymous(FileWatcher(directory))
          context.watch(fileWatcher)
        }
        Behaviors
          .receiveMessage[Nothing] {
            case _: Any =>
              Behaviors.unhandled
          }
          .receiveSignal {
            case (context, Terminated(actorRef)) =>
              // checks not all fileWatcher had Terminated
              // if no fileWatcher left shuts down the system
              Behaviors.same
          }
      }
}

object FileWatcher extends FileWatchingAbilities {

  sealed trait Command
  case class NewFile(file: File, timeAdded: Long) extends Command
  case class FileModified(file: File, timeAdded: Long) extends Command

  def apply(directory: String): Behavior[Command] =
    Behaviors
      .supervise {
        Behaviors.setup[Command] { context =>
          //starts listening to directory
          //for event new file sends NewFile to itself
          //for event file modifid send FileModified to itself
          // spawns LogProcessor
          // watches LogProcessor
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
  //after restart work can get duplicated, dedup is out of the scope.

}

//provides File watching API
trait FileWatchingAbilities

object LogProcessor {

  sealed trait Command
  case class LogFile(file: File) extends Command

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

object DbWriter {

  sealed trait Command
  case class Line(time: Long, message: String, messageType: String)
      extends Command

  def apply(databaseUrl: String): Behavior[Command] =
    supervisorStrategy {
      Behaviors.setup[Command] { context =>
        // creates connection using databaseUrl
        Behaviors
          .receiveMessage[Command] {
            case Line(t, m, mt) => ???
            //transforms line to db schema
            //saves to db
          }
          .receiveSignal {
            case (_, PostStop) => ???
            //close connection
            case (_, PreRestart) => ???
            //close connection
          }
      }
    }

  def supervisorStrategy(
      behavior: Behavior[Command]): Behavior[Command] =
    Behaviors
      .supervise {
        Behaviors
          .supervise(behavior)
          .onFailure[UnexpectedColumnsException](
            SupervisorStrategy.resume)
      }
      .onFailure[DbBrokenConnectionException](
        SupervisorStrategy
          .restartWithBackoff(
            minBackoff = 3.seconds,
            maxBackoff = 30.seconds,
            randomFactor = 0.1)
          .withResetBackoffAfter(15.seconds))
}

//FileWatcherCapabilities exception
@SerialVersionUID(1L)
class ClosedWatchServiceException(msg: String)
    extends Exception(msg)
    with Serializable

//LogProcessor's parser exception
@SerialVersionUID(1L)
class ParseException(msg: String, val file: File)
    extends Exception(msg)
    with Serializable

//DbWriter's connector exceptions
@SerialVersionUID(1L)
class DbBrokenConnectionException(msg: String)
    extends Exception(msg)
    with Serializable

@SerialVersionUID(1L)
class UnexpectedColumnsException(msg: String)
    extends Exception(msg)
    with Serializable
