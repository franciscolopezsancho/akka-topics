package faulttolerance3

import akka.actor.typed.Behavior
import akka.actor.typed.PostStop
import akka.actor.typed.PreRestart
import akka.actor.typed.SupervisorStrategy
import akka.actor.typed.scaladsl.Behaviors
import faulttolerance3.exception.DbBrokenConnectionException
import faulttolerance3.exception.UnexpectedColumnsException

import scala.concurrent.duration._

object DbWriter {

  sealed trait Command

  final case class Line(
      time: Long,
      message: String,
      messageType: String)
      extends Command

  def apply(databaseUrl: String): Behavior[Command] =
    supervisorStrategy {
      Behaviors.setup[Command] { context =>
        // creates connection using databaseUrl
        Behaviors
          .receiveMessage[Command] {
            case Line(t, m, mt) => ???
            //transforms line to db schema
            //saves to db
          }
          .receiveSignal {
            case (_, PostStop) => ???
            //close connection
            case (_, PreRestart) => ???
            //close connection
          }
      }
    }

  def supervisorStrategy(
      behavior: Behavior[Command]): Behavior[Command] =
    Behaviors
      .supervise {
        Behaviors
          .supervise(behavior)
          .onFailure[UnexpectedColumnsException](
            SupervisorStrategy.resume)
      }
      .onFailure[DbBrokenConnectionException](
        SupervisorStrategy
          .restartWithBackoff(
            minBackoff = 3.seconds,
            maxBackoff = 30.seconds,
            randomFactor = 0.1)
          .withResetBackoffAfter(15.seconds))
}
