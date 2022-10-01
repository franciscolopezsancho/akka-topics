package ask.content

import akka.actor.typed.scaladsl.{ ActorContext, Behaviors }
import akka.actor.typed.{ ActorRef, ActorSystem, Behavior }
import scala.concurrent.duration.SECONDS
import akka.util.Timeout
import scala.util.{ Failure, Random, Success }

object QuestionWithPayload extends App {

  val guardian: ActorSystem[Guardian.Command] =
    ActorSystem(Guardian(), "LoadedQuestion")
  guardian ! Guardian.Start

  println("press ENTER to terminate")
  scala.io.StdIn.readLine()
  guardian.terminate()
}

object Guardian {

  def apply(): Behavior[Command] =
    Behaviors.setup { context =>
      val manager: ActorRef[Manager.Command] =
        context.spawn(Manager(), "manager-1")
      Behaviors.receiveMessage { message =>
        manager ! Manager.Delegate(List("text-a", "text-b", "text-c"))
        Behaviors.same
      }
    }

  sealed trait Command
  final case object Start extends Command
}

object Manager {

  sealed trait Command
  final case class Delegate(texts: List[String]) extends Command
  final case class Report(outline: String) extends Command

  def apply(): Behavior[Command] =
    Behaviors.setup { context =>
      implicit val timeout: Timeout = Timeout(1, SECONDS)
      def auxCreateRequest(text: String)(
          replyTo: ActorRef[Worker.Response]): Worker.Parse =
        Worker.Parse(text, replyTo)

      Behaviors.receiveMessage { message =>
        message match {
          case Delegate(texts) =>
            texts.map { text =>
              val worker: ActorRef[Worker.Command] =
                context.spawn(Worker(), s"worker-$text")
              context.ask(worker, auxCreateRequest(text)) {
                case Success(_) =>
                  Report(s"$text read by ${worker.path.name}")
                case Failure(ex) =>
                  Report(
                    s"reading '$text' has failed with [${ex.getMessage()}")
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

object Worker {

  sealed trait Command
  final case class Parse(
      text: String,
      replyTo: ActorRef[Worker.Response])
      extends Command

  sealed trait Response
  final case object Done extends Response

  def apply(): Behavior[Command] =
    Behaviors.receive { (context, message) =>
      message match {
        case Parse(text, replyTo) =>
          fakeLengthyParsing(text)
          context.log.info(s"${context.self.path.name}: done")
          replyTo ! Worker.Done
          Behaviors.same
      }
    }

  private def fakeLengthyParsing(text: String): Unit = {
    val endTime =
      System.currentTimeMillis + Random.between(2000, 4000)
    while (endTime > System.currentTimeMillis) {}
  }
}
