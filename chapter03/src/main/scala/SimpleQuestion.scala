package ask

import akka.actor.typed.scaladsl.{ ActorContext, Behaviors }
import akka.actor.typed.{ ActorRef, ActorSystem, Behavior }
import scala.concurrent.duration.SECONDS
import akka.util.Timeout
import scala.util.{ Failure, Random, Success }

object SimpleQuestion extends App {

  val guardian: ActorSystem[Guardian.Command] =
    ActorSystem(Guardian(), "example-ask-without-content")
  guardian ! Guardian.Start(List("text-a", "text-b", "text-c"))

  println("press ENTER to terminate")
  scala.io.StdIn.readLine()
  guardian.terminate()
}

object Guardian {

  sealed trait Command
  case class Start(texts: List[String]) extends Command

  def apply(): Behavior[Command] =
    Behaviors.setup { context =>
      val manager: ActorRef[Manager.Command] =
        context.spawn(Manager(), "manager-1")
      Behaviors.receiveMessage {
        case Start(texts) =>
          manager ! Manager.Delegate(texts)
          Behaviors.same
      }
    }
}

object Manager {

  sealed trait Command
  case class Delegate(texts: List[String]) extends Command
  private case class Report(description: String) extends Command

  def apply(): Behavior[Command] =
    Behaviors.setup { context =>
      implicit val timeout: Timeout = Timeout(3, SECONDS)

      Behaviors.receiveMessage { message =>
        message match {
          case Delegate(texts) =>
            texts.map { text =>
              val worker: ActorRef[Worker.Command] =
                context.spawn(Worker(text), s"worker-$text")
              context.ask(worker, Worker.Parse) {
                case Success(Worker.Done) =>
                  Report(s"$text read by ${worker.path.name}")
                case Failure(ex) =>
                  Report(
                    s"reading '$text' has failed with [${ex.getMessage()}")
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

object Worker {

  sealed trait Command
  case class Parse(replyTo: ActorRef[Worker.Response]) extends Command

  sealed trait Response
  case object Done extends Response

  def apply(text: String): Behavior[Command] =
    Behaviors.receive { (context, message) =>
      message match {
        case Parse(replyTo) =>
          fakeLengthyParsing(text)
          prettyPrint(context, "DONE!")
          replyTo ! Worker.Done
          Behaviors.same
      }
    }

  def fakeLengthyParsing(text: String): Unit = {
    val endTime =
      System.currentTimeMillis + Random.between(2000, 4000)
    while (endTime > System.currentTimeMillis) {}
  }

  def prettyPrint(context: ActorContext[_], message: String): Unit = {
    context.log.info(s"${context.self.path.name}: $message")
  }
}
