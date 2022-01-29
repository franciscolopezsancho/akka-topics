package com.manning.fsm

import akka.actor.typed.scaladsl.{ ActorContext, Behaviors }
import akka.actor.typed.{ ActorRef, ActorSystem, Behavior }

object FSMApp extends App {

  val system: ActorSystem[Guardian.Command] =
    ActorSystem(Guardian(), "finite-state-machine")
  system ! Guardian.Start(List("one", "two", "three"))

}

object Guardian {

  sealed trait Command
  case class Start(tasks: List[String]) extends Command

  def apply(): Behavior[Command] =
    Behaviors.setup { context =>
      val manager: ActorRef[Manager.Command] =
        context.spawn(Manager(1), "manager-1")
      Behaviors.receiveMessage {
        case Start(tasks) =>
          tasks.map { task =>
            manager ! Manager.Delegate(task)
          }
          Behaviors.same
      }
    }
}

object Manager {

  sealed trait Command
  final case class Delegate(task: String) extends Command
  final case class AdapterPrinterResponse(message: Printer.Response)
      extends Command

  def apply(manPower: Int): Behavior[Command] =
    Behaviors.setup { context =>

      val printers: Seq[ActorRef[Printer.Command]] =
        (0 to manPower).map(num =>
          context.spawn(Printer(), s"printer-$num"))
      val adapter: ActorRef[Printer.Response] =
        context.messageAdapter(rsp => AdapterPrinterResponse(rsp))

      Behaviors.receiveMessage { message =>
        message match {
          case Delegate(task) =>
            val printer =
              printers(scala.util.Random.nextInt(printers.size))
            printer ! Printer.Do(adapter, task)
          case AdapterPrinterResponse(response) => {
            response match {
              case Printer.Cleaning(task) =>
                context.self ! Delegate(task)
              case Printer.JobDone(task) =>
                context.log.info(s"task '$task' has been finished")
            }
          }
        }
        Behaviors.same
      }
    }
}

object Printer {

  sealed trait Command
  final case class Do(
      replyTo: ActorRef[Printer.Response],
      task: String)
      extends Command
  private final case object Clean extends Command

  sealed trait Response
  final case class Cleaning(task: String) extends Response
  final case class JobDone(task: String) extends Response

  def apply(): Behavior[Command] =
    ready(0)

  def cleaning(jobsDone: Int): Behavior[Command] =
    Behaviors.receive[Command] { (context, message) =>
      message match {
        case Do(manager, task) =>
          prettyPrint(context, s"can't do [$task]. Cleaning needed")
          manager ! Cleaning(task)
          Behaviors.same
        case Clean =>
          doing(
            1000,
            context,
            "I'm DONE cleaning"
          ) // this represent some work
          ready(jobsDone)
      }
    }

  def ready(jobsDone: Int): Behavior[Command] =
    Behaviors.receive[Command] { (context, message) =>
      message match {
        case Do(manager, task) =>
          doing(
            999,
            context,
            s"Done [${jobsDone + 1}] job(s)"
          ) // this represent some work
          manager ! JobDone(task)
          context.self ! Clean
          cleaning(jobsDone + 1)
        case Clean => Behaviors.unhandled
      }
    }

  def doing(
      duration: Int,
      context: ActorContext[_],
      message: String): Unit = {
    val endTime = System.currentTimeMillis + duration
    while (endTime > System.currentTimeMillis) {}
    prettyPrint(context, message)
  }

  def prettyPrint(context: ActorContext[_], message: String): Unit = {
    context.log.info(s"${context.self.path.name}': $message")
  }

}
