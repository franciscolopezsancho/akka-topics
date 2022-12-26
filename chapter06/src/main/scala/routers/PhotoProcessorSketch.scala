package routers

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.receptionist.ServiceKey
import akka.actor.typed.scaladsl.Behaviors

object PhotoProcessorSketch {

  val key = ServiceKey[String]("photo-processor-key")

  sealed trait Command
  final case class File(
      location: String,
      camera: ActorRef[Camera.Photo])
      extends Command
  final case object Done extends Command

  def apply(): Behavior[Command] = ready()
  def ready(): Behavior[Command] = Behaviors.receiveMessage[Command] {
    case f: File =>
      //processing the photo and when finish back to ready() state.
      // meanwhile to busy
      busy()
  }

  def busy(): Behavior[Command] = Behaviors.receiveMessage[Command] {
    case File(location, camera) =>
      //can't process the file sends back the photo to the camera
      camera ! Camera.Photo(location)
      Behaviors.same
    case Done =>
      ready()
  }
}
