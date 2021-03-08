package ask.content

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorRef, ActorSystem, Behavior }
import scala.concurrent.duration.SECONDS
import akka.util.Timeout
import scala.util.{ Failure, Success }

object ManangerWorkerApp extends App {

  val system: ActorSystem[Guardian.Command] =
    ActorSystem(Guardian(), "example-ask-with-content")
  system ! Guardian.Start
}

object Guardian {

  def apply(): Behavior[Command] =
    Behaviors.setup { context =>
      val manager: ActorRef[Manager.Command] =
        context.spawn(Manager(), "manager-1")
      Behaviors.receiveMessage { message =>
        manager ! Manager.Delegate(
          List("task-a", "task-b", "task-c", "task-d"))
        Behaviors.same
      }
    }

  sealed trait Command
  case object Start extends Command
}

object Manager {

  sealed trait Command
  final case class Delegate(tasks: List[String]) extends Command
  final case class Report(outline: String) extends Command

  def apply(): Behavior[Command] =
    Behaviors.setup { context =>
      println(context.self.path)
      implicit val timeout: Timeout = Timeout(1, SECONDS)
      def auxCreateRequest(task: Worker.Task)(
          replyTo: ActorRef[Worker.Response]): Worker.Do =
        Worker.Do(task, replyTo)

      Behaviors.receiveMessage { message =>
        message match {
          case Delegate(tasks) =>
            tasks.map { task =>
              val worker: ActorRef[Worker.Command] =
                context.spawn(Worker(), s"worker-$task")
              context.ask(
                worker,
                auxCreateRequest(
                  Worker.Task(
                    System.currentTimeMillis().toString(),
                    task))) {
                case Success(Worker.Done(taskId)) =>
                  Report(s"$taskId has been finished by ${worker}")
                case Failure(ex) =>
                  Report(s"task has failed with [${ex.getMessage()}")
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

  final case class Task(taskId: String, taskDescription: String)

  sealed trait Command
  final case class Do(task: Task, response: ActorRef[Worker.Response])
      extends Command

  sealed trait Response
  final case class Done(taskId: String) extends Response

  def apply(): Behavior[Command] =
    Behaviors.receive { (context, message) =>
      message match {
        case Do(task, replyTo) =>
          doing(scala.util.Random.between(2000, 4000))
          context.log.info(
            s"My name is '${context.self.path}'. And I've done ${task}")
          replyTo ! Worker.Done(task.taskId)
          Behaviors.stopped
      }
    }

  def doing(duration: Int): Unit = {
    val endTime = System.currentTimeMillis + duration
    while (endTime > System.currentTimeMillis) {}
  }
}
