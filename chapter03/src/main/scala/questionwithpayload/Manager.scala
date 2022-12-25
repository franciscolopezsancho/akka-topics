package questionwithpayload

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.util.Timeout

import scala.concurrent.duration.SECONDS
import scala.util.Failure
import scala.util.Success

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
