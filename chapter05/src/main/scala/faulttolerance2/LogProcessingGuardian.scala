package faulttolerance2

import akka.actor.typed.ActorRef
import akka.actor.typed.Terminated
import akka.actor.typed.scaladsl.Behaviors

object LogProcessingGuardian {

  def apply(sources: Vector[String], databaseUrl: String) =
    Behaviors
      .setup[Nothing] { context =>
        sources.foreach { source =>
          val dbWriter: ActorRef[DbWriter.Command] =
            context.spawnAnonymous(DbWriter(databaseUrl))
          val logProcessor: ActorRef[
            LogProcessor.Command
          ] = // wouldn't it be better to have more log processors
            context.spawnAnonymous(LogProcessor(dbWriter))
          val fileWatcher: ActorRef[FileWatcher.Command] =
            context.spawnAnonymous(FileWatcher(source, logProcessor))
          context.watch(fileWatcher)
        }
        Behaviors
          .receiveMessage[Nothing] {
            case _: Any =>
              Behaviors.ignore
          }
          .receiveSignal {
            case (context, Terminated(actorRef)) =>
              // checks there is some fileWatcher running
              // if there's no fileWatcher left
              //then shutsdown the system
              Behaviors.same
          }
      }
}
