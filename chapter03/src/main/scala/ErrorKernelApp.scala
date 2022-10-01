package errorkernel

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorRef, ActorSystem, Behavior }

object ErrorKernelApp extends App {

  val guardian: ActorSystem[Guardian.Command] =
    ActorSystem(Guardian(), "error-kernel")
  guardian ! Guardian.Start(List("-one-", "--two--"))

  println("press ENTER to terminate")
  scala.io.StdIn.readLine()
  guardian.terminate()
}

object Guardian {

  sealed trait Command
  final case class Start(texts: List[String]) extends Command

  def apply(): Behavior[Command] =
    Behaviors.setup { context =>
      context.log.info("Setting up. Creating manager")
      val manager: ActorRef[Manager.Command] =
        context.spawn(Manager(), "manager-alpha")
      Behaviors.receiveMessage {
        case Start(texts) =>
          manager ! Manager.Delegate(texts)
          Behaviors.same
      }
    }
}

object Manager {

  sealed trait Command
  final case class Delegate(texts: List[String]) extends Command
  private final case class WorkerDoneAdapter(
      response: Worker.Response)
      extends Command

  def apply(): Behavior[Command] =
    Behaviors.setup { context =>
      val adapter: ActorRef[Worker.Response] =
        context.messageAdapter(response =>
          WorkerDoneAdapter(response))

      Behaviors.receiveMessage { message =>
        message match {
          case Delegate(texts) =>
            texts.map { text =>
              val worker: ActorRef[Worker.Command] =
                context.spawn(Worker(), s"worker$text")
              context.log.info(s"sending text '${text}' to worker")
              worker ! Worker.Parse(adapter, text)
            }
            Behaviors.same
          case WorkerDoneAdapter(Worker.Done(text)) =>
            context.log.info(s"text '$text' has been finished")
            Behaviors.same
        }
      }
    }
}

object Worker {

  sealed trait Command
  final case class Parse(
      replyTo: ActorRef[Worker.Response],
      text: String)
      extends Command

  sealed trait Response
  final case class Done(text: String) extends Response

  def apply(): Behavior[Command] =
    Behaviors.receive { (context, message) =>
      message match {
        case Parse(replyTo, text) =>
          val parsed = naiveParsing(text)
          context.log.info(
            s"'${context.self}' DONE!. Parsed result: $parsed")
          replyTo ! Worker.Done(text)
          Behaviors.stopped
      }
    }

  def naiveParsing(text: String): String =
    text.replaceAll("-", "")

}
