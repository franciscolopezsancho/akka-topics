import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorSystem, Behavior }

object HalloweenChild {

  sealed trait Treat

  case class Candy(name: String) extends Treat
  case object NonCandy extends Treat

  def apply(): Behavior[Treat] =
    Behaviors.receive { (context, message) =>
      message match {
        case NonCandy =>
          context.log.info("this is not fun. I'm going home")
          Behaviors.stopped

        case Candy(name: String) =>
          context.log.info(s"this is so much fun. I love $name")
          Behaviors.same
      }
    }
}

object HalloweenChildApp extends App {

  val guardian: ActorSystem[HalloweenChild.Treat] =
    ActorSystem(HalloweenChild(), "halloween")
  guardian ! HalloweenChild.Candy("chocolate bar")
  guardian ! HalloweenChild.NonCandy
  guardian ! HalloweenChild.Candy("chocolate bar")

}
