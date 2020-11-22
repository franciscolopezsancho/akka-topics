package stashy

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorRef, ActorSystem, Behavior }

object ManangerWorkerApp extends App {

  import Guardian._

  val system: ActorSystem[Start] = ActorSystem(Guardian(), "guardian")
  system ! Start()

}

object Guardian {

  def apply(): Behavior[Start] =
    Behaviors.setup { context =>
      context.log.info("setting up!")
      //bear in mind we count form zero when Manager(1)
      val manager: ActorRef[Manager.Command] =
        context.spawn(Manager(1), "manager-1")
      val tasks: List[String] = List("one", "two", "three")
      Behaviors.receiveMessage { message =>
        tasks.map { task =>
          manager ! Manager.Delegate1(task)
        }
        Behaviors.same
      }
    }

  //why can't I pass an object?
  case class Start()

}

object Manager {

  def apply(manPower: Int): Behavior[Command] =
    Behaviors.setup { context =>
      val workers: Seq[ActorRef[Worker.Command]] =
        (0 to manPower).map(num => context.spawn(Worker(), s"worker-$num"))
      Behaviors.receiveMessage { message =>
        message match {
          case Delegate1(task) =>
            val worker = workers(scala.util.Random.nextInt(workers.size))
            worker ! Worker.Do(context.self, task)
          case Done(task) =>
            context.log.info(s"task '$task' has been finished")
        }
        Behaviors.same
      }
    }

  sealed trait Command
  final case class DelegateN(tasks: List[String]) extends Command
  final case class Delegate1(task: String) extends Command
  final case class Done(task: String) extends Command

}

object Worker {

  def apply(): Behavior[Command] =
    idle(0)

  def cleaning(jobsDone: Int): Behavior[Command] =
    Behaviors.withStash(100) { buffer =>
      Behaviors.receive[Command] { (context, message) =>
        message match {
          case Do(manager, task) =>
            context.log.info(
              s"I'm ${context.self.path}, I'll STASH it, I need to clean first")
            buffer.stash(message)
            Behaviors.same
          case Clean =>
            doing(1000) // this represent some work
            context.log.info(s"I'm ${context.self.path}, I'm DONE cleaning")
            buffer.unstashAll(idle(jobsDone))
        }
      }
    }

  def idle(jobsDone: Int): Behavior[Command] =
    Behaviors.receive[Command] { (context, message) =>
      message match {
        case Do(manager, task) =>
          doing(999) // this represent some work
          val jobSoFar = jobsDone + 1
          context.log.info(
            s"I'm ${context.self.path}'. I've done $jobSoFar job(s) so far")
          manager ! Manager.Done(task)
          context.self ! Clean
          cleaning(jobSoFar)
        case Clean =>
          context.self ! Clean
          cleaning(jobsDone)
      }
    }

  sealed trait Command
  final case class Do(replyTo: ActorRef[Manager.Command], task: String)
      extends Command
  final case object Clean extends Command

  def doing(duration: Int): Unit = {
    val endTime = System.currentTimeMillis + duration
    while (endTime > System.currentTimeMillis) {}
  }

}
