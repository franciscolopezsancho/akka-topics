import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorSystem, Behavior }

object Recorder {

  sealed trait Storage

  case class S3(record: String) extends Storage
  case class AzureBlobStorage(record: String) extends Storage

  def apply(): Behavior[Storage] =
    Behaviors.receive { (context, message) =>
      message match {
        case S3(record) =>
          context.log.info(s"saving $record in S3")
          Behaviors.same
        case AzureBlobStorage(record) =>
          context.log.info(s"saving $record in Azure Blob Storage")
          Behaviors.same
      }
    }
}

object StoringApp extends App {

  val guardian: ActorSystem[Recorder.Storage] =
    ActorSystem(Recorder(), "halloween")
  guardian ! Recorder.S3("file 1")
  guardian ! Recorder.AzureBlobStorage("file 1")
  guardian ! Recorder.S3("file 2")

}
