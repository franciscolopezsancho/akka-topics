package com.manning.fsm

import akka.actor.typed.scaladsl.Behaviors
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
  final case class Done(task: String) extends Command
  final case class AdapterWorkerResponse(message: Worker.Response)
      extends Command

  def apply(manPower: Int): Behavior[Command] =
    Behaviors.setup { context =>

      val workers: Seq[ActorRef[Worker.Command]] =
        (0 to manPower).map(num =>
          context.spawn(Worker(), s"worker-$num"))
      val adapter: ActorRef[Worker.Response] =
        context.messageAdapter(rsp => AdapterWorkerResponse(rsp))

      Behaviors.receiveMessage { message =>
        message match {
          case Delegate(task) =>
            val worker =
              workers(scala.util.Random.nextInt(workers.size))
            worker ! Worker.Do(adapter, task)
          case Done(task) =>
            context.log.info(s"task '$task' has been finished")
          case AdapterWorkerResponse(response) => {
            response match {
              case Worker.Cleaning(task) =>
                context.self ! Delegate(task)
              case Worker.JobDone(task) =>
                context.self ! Done(task)
            }
          }
        }
        Behaviors.same
      }
    }
}

object Worker {

  sealed trait Command
  final case class Do(
      replyTo: ActorRef[Worker.Response],
      task: String)
      extends Command
  final case object Clean extends Command

  sealed trait Response
  final case class Cleaning(task: String) extends Response
  final case class JobDone(task: String) extends Response

  def apply(): Behavior[Command] =
    idle(0)

  def cleaning(jobsDone: Int): Behavior[Command] =
    Behaviors.receive[Command] { (context, message) =>
      message match {
        case Do(manager, task) =>
          context.log.info(
            s"${context.self.path.name}: can't do '$task' now. I need to clean first")
          manager ! Cleaning(task)
          Behaviors.same
        case Clean =>
          doing(1000) // this represent some work
          context.log.info(
            s"${context.self.path.name}: I'm DONE cleaning")
          idle(jobsDone)
      }
    }

  def idle(jobsDone: Int): Behavior[Command] =
    Behaviors.receive[Command] { (context, message) =>
      message match {
        case Do(manager, task) =>
          doing(999) // this represent some work
          val jobSoFar = jobsDone + 1
          context.log.info(
            s"${context.self.path.name}': I've done '$jobSoFar' job(s) so far")
          manager ! JobDone(task)
          context.self ! Clean
          cleaning(jobSoFar)
        case Clean =>
          context.log.info(
            s"${context.self.path.name} : nothing to clean yet")
          Behaviors.same
      }
    }

  def doing(duration: Int): Unit = {
    val endTime = System.currentTimeMillis + duration
    while (endTime > System.currentTimeMillis) {}
  }

}
