package ask.content

import akka.actor.typed.scaladsl.{ ActorContext, Behaviors }
import akka.actor.typed.{ ActorRef, ActorSystem, Behavior }
import scala.concurrent.duration.SECONDS
import akka.util.Timeout
import scala.util.{ Failure, Random, Success }

object LoadedQuestion extends App {

  val system: ActorSystem[Guardian.Command] =
    ActorSystem(Guardian(), "LoadedQuestion")
  system ! Guardian.Start
}

object Guardian {

  def apply(): Behavior[Command] =
    Behaviors.setup { context =>
      val manager: ActorRef[Manager.Command] =
        context.spawn(Manager(), "manager-1")
      Behaviors.receiveMessage { message =>
        manager ! Manager.Delegate(List("file-a", "file-b", "file-c"))
        Behaviors.same
      }
    }

  sealed trait Command
  case object Start extends Command
}

object Manager {

  sealed trait Command
  final case class Delegate(files: List[String]) extends Command
  final case class Report(outline: String) extends Command

  def apply(): Behavior[Command] =
    Behaviors.setup { context =>
      implicit val timeout: Timeout = Timeout(1, SECONDS)
      def auxCreateRequest(file: String)(
          replyTo: ActorRef[Reader.Response]): Reader.Read =
        Reader.Read(file, replyTo)

      Behaviors.receiveMessage { message =>
        message match {
          case Delegate(files) =>
            files.map { file =>
              val reader: ActorRef[Reader.Command] =
                context.spawn(Reader(), s"reader-$file")
              context.ask(reader, auxCreateRequest(file)) {
                case Success(_) =>
                  Report(s"$file read by ${reader.path.name}")
                case Failure(ex) =>
                  Report(
                    s"reading '$file' has failed with [${ex.getMessage()}")
              }
            }
            Behaviors.same
          case Report(outline) =>
            context.log.info(outline)
            Behaviors.same
        }
      }
    }
}

object Reader {

  sealed trait Command
  case class Read(file: String, replyTo: ActorRef[Reader.Response])
      extends Command

  sealed trait Response
  case object Done extends Response

  def apply(): Behavior[Command] =
    Behaviors.receive { (context, message) =>
      message match {
        case Read(file, replyTo) =>
          fakeReading(file)
          prettyPrint(
            context,
            s"$file done"
          ) // to show that it's done when delay
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
