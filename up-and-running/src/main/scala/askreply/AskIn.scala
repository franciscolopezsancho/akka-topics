package ask.simple

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorRef, ActorSystem, Behavior }
import scala.concurrent.duration.SECONDS
import akka.util.Timeout
import scala.util.{ Failure, Success }

object ManangerWorkerApp extends App {

  val system: ActorSystem[Guardian.Command] =
    ActorSystem(Guardian(), "example-ask-without-content")
  system ! Guardian.Start
}

object Guardian {

  def apply(): Behavior[Command] =
    Behaviors.setup { context =>
      val manager: ActorRef[Manager.Command] =
        context.spawn(Manager(), "manager-1")
      Behaviors.receiveMessage { message =>
        manager ! Manager.Delegate(List("task-a", "task-b", "task-c", "task-d"))
        Behaviors.same
      }
    }

  sealed trait Command
  case object Start extends Command
}

object Manager {

  sealed trait Command
  case class Delegate(tasks: List[String]) extends Command
  case class Report(description: String) extends Command

  def apply(): Behavior[Command] =
    Behaviors.setup { context =>
      implicit val timeout: Timeout = Timeout(3, SECONDS)

      Behaviors.receiveMessage { message =>
        message match {
          case Delegate(tasks) =>
            tasks.map { task =>
              val worker: ActorRef[Worker.Command] =
                context.spawn(Worker(), s"worker-$task")
              context.ask(worker, Worker.Do) {
                case Success(Worker.Done) =>
                  Report(s"$task has been finished by ${worker.path.name}")
                case Failure(ex) =>
                  Report(s"task '$task' has failed with [${ex.getMessage()}")
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
  case class Do(replyTo: ActorRef[Worker.Response]) extends Command

  sealed trait Response
  case object Done extends Response

  def apply(): Behavior[Command] =
    Behaviors.receive { (context, message) =>
      message match {
        case Do(replyTo) =>
          throw new IllegalArgumentException("yeye")
          doing(scala.util.Random.between(2000, 4000))
          context.log.info(
            s"My name is '${context.self.path.name}'. And I've done my task")
          replyTo ! Worker.Done
          Behaviors.stopped
      }
    }

  def doing(duration: Int): Unit = {
    val endTime = System.currentTimeMillis + duration
    while (endTime > System.currentTimeMillis) {}
  }
}
