package ask.simple

import akka.actor.typed.scaladsl.{ ActorContext, Behaviors }
import akka.actor.typed.{ ActorRef, ActorSystem, Behavior }
import scala.concurrent.duration.SECONDS
import akka.util.Timeout
import scala.util.{ Failure, Random, Success }

object SimpleQuestion extends App {

  val system: ActorSystem[Guardian.Command] =
    ActorSystem(Guardian(), "example-ask-without-content")
  system ! Guardian.Start(List("file-a", "file-b", "file-c"))
}

object Guardian {

  sealed trait Command
  case class Start(files: List[String]) extends Command

  def apply(): Behavior[Command] =
    Behaviors.setup { context =>
      val manager: ActorRef[Manager.Command] =
        context.spawn(Manager(), "manager-1")
      Behaviors.receiveMessage {
        case Start(files) =>
          manager ! Manager.Delegate(files)
          Behaviors.same
      }
    }
}

object Manager {

  sealed trait Command
  case class Delegate(files: List[String]) extends Command
  case class Report(description: String) extends Command

  def apply(): Behavior[Command] =
    Behaviors.setup { context =>
      implicit val timeout: Timeout = Timeout(3, SECONDS)

      Behaviors.receiveMessage { message =>
        message match {
          case Delegate(files) =>
            files.map { file =>
              val reader: ActorRef[Reader.Command] =
                context.spawn(Reader(file), s"reader-$file")
              context.ask(reader, Reader.Read) {
                case Success(Reader.Done) =>
                  Report(s"$file read by ${reader.path.name}")
                case Failure(ex) =>
                  Report(
                    s"reading '$file' has failed with [${ex.getMessage()}")
              }
            }
            Behaviors.same
          case Report(description) =>
            context.log.info(description)
            Behaviors.same
        }
      }
    }
}

object Reader {

  sealed trait Command
  case class Read(replyTo: ActorRef[Reader.Response]) extends Command

  sealed trait Response
  case object Done extends Response

  def apply(file: String): Behavior[Command] =
    Behaviors.receive { (context, message) =>
      message match {
        case Read(replyTo) =>
          fakeReading(file)
          prettyPrint(context, "done")
          replyTo ! Reader.Done
          Behaviors.same
      }
    }

  def fakeReading(file: String): Unit = {
    val endTime =
      System.currentTimeMillis + Random.between(2000, 4000)
    while (endTime > System.currentTimeMillis) {}
  }

  def prettyPrint(context: ActorContext[_], message: String): Unit = {
    context.log.info(s"${context.self.path.name}: $message")
  }
}
