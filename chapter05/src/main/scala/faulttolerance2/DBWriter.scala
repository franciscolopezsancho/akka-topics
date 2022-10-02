package faulttolerance2

import akka.actor.typed.Behavior
import akka.actor.typed.PostStop
import akka.actor.typed.PreRestart
import akka.actor.typed.SupervisorStrategy
import akka.actor.typed.scaladsl.Behaviors
import faulttolerance2.exception.DbBrokenConnectionException
import faulttolerance2.exception.DbNodeDownException

import scala.concurrent.duration._

object DbWriter {

  sealed trait Command
  final case class Line(
      time: Long,
      message: String,
      messageType: String)
      extends Command

  def apply(databaseUrl: String): Behavior[Command] =
    supervisonStrategy {
      Behaviors.setup[Command] { context =>
        // creates connection with databaseUrl)
        Behaviors
          .receiveMessage[Command] {
            case Line(t, m, mt) => ???
            //saves line to db
          }
          .receiveSignal {
            case (_, PostStop) => ???
            //close connection
            case (_, PreRestart) => ???
            //close connection
          }
      }
    }

  def supervisonStrategy(beh: Behavior[Command]): Behavior[Command] =
    Behaviors
      .supervise {
        Behaviors
          .supervise {
            beh
          }
          .onFailure[DbBrokenConnectionException](
            SupervisorStrategy.restart)
      }
      .onFailure[DbNodeDownException](SupervisorStrategy.stop)

}
