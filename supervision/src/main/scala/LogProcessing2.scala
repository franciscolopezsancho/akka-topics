package aia.faulttolerance2

import java.io.File
import java.util.UUID
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
import language.postfixOps

object LogProcessingApp extends App {
  val sources = Vector("file:///source1/", "file:///source2/")
  val databaseUrl = "http://mydatabase1"

  val guardian = ActorSystem[Nothing](
    LogProcessingGuardian(sources, databaseUrl),
    "log-processing-app")
}

object LogProcessingGuardian {

  def apply(sources: Vector[String], databaseUrl: String) =
    Behaviors
      .setup[Nothing] { context =>
        sources.foreach { source =>
          val dbWriter: ActorRef[DbWriter.Command] =
            context.spawnAnonymous(DbWriter(databaseUrl))
          val logProcessor: ActorRef[
            LogProcessor.Command
          ] = // wouldn't it be better to have more log processors
            context.spawnAnonymous(LogProcessor(dbWriter))
          val fileWatcher: ActorRef[FileWatcher.Command] =
            context.spawnAnonymous(FileWatcher(source, logProcessor))
          context.watch(fileWatcher)
        }
        Behaviors
          .receiveMessage[Nothing] {
            case _: Any =>
              Behaviors.unhandled
          }
          .receiveSignal {
            case (context, Terminated(actorRef)) =>
              // checks there is some fileWatcher running
              // if there's no fileWatcher left
              //then shutsdown the system
              Behaviors.same
          }
      }
}

object FileWatcher {

  sealed trait Command
  case class NewFile(file: File, timeAdded: Long) extends Command

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

object LogProcessor {

  sealed trait Command
  case class LogFile(file: File) extends Command

  def apply(dbWriter: ActorRef[DbWriter.Command]): Behavior[Command] =
    Behaviors
      .supervise {
        Behaviors.receiveMessage[Command] {
          case LogFile(file) => ???
          //parses file and sends each line to dbWriter
        }
      }
      .onFailure[CorruptedFileException](SupervisorStrategy.resume)
}

object DbWriter {

  sealed trait Command
  case class Line(time: Long, message: String, messageType: String)
      extends Command

  def apply(databaseUrl: String): Behavior[Command] =
    supervisonStrategy {
      Behaviors.setup[Command] { context =>
        // creates connection with databaseUrl)
        Behaviors
          .receiveMessage[Command] {
            case Line(t, m, mt) => ???
            //saves line to db
          }
          .receiveSignal {
            case (_, PostStop) => ???
            //close connection
            case (_, PreRestart) => ???
            //close connection
          }
      }
    }

  def supervisonStrategy(beh: Behavior[Command]): Behavior[Command] =
    Behaviors
      .supervise {
        Behaviors
          .supervise {
            beh
          }
          .onFailure[DbBrokenConnectionException](
            SupervisorStrategy.restart)
      }
      .onFailure[DbNodeDownException](SupervisorStrategy.stop)

}

@SerialVersionUID(1L)
class DiskError(msg: String) extends Error(msg) with Serializable

@SerialVersionUID(1L)
class CorruptedFileException(msg: String, val file: File)
    extends Exception(msg)
    with Serializable

@SerialVersionUID(1L)
class DbBrokenConnectionException(msg: String)
    extends Exception(msg)
    with Serializable

@SerialVersionUID(1L)
class DbNodeDownException(msg: String)
    extends Exception(msg)
    with Serializable
