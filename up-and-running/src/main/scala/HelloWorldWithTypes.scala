import akka.actor.typed.{ ActorSystem, Behavior }
import akka.actor.typed.scaladsl.Behaviors

object HelloWorldWithTypes {

  def apply(): Behavior[Message] =
    Behaviors.receive[Message] { (context, msg) =>
      msg match {
        case Message(content) =>
          context.log.info(s"the message is.... $content")
      }
      Behaviors.stopped
    }

  case class Message(content: String)
}

object HelloWorld2App extends App {

  val system: ActorSystem[HelloWorldWithTypes.Message] =
    ActorSystem(HelloWorldWithTypes(), "HelloWorldWithTypes")
  system ! HelloWorldWithTypes.Message("hi")

}
