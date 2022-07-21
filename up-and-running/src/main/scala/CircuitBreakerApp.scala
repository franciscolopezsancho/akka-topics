package com.manning.circuitbreaker

import akka.actor.typed.scaladsl.{ ActorContext, Behaviors }
import akka.actor.typed.{ ActorRef, ActorSystem, Behavior }
import scala.concurrent.duration._

object CircuitBreakerApp extends App {

  import CircuitBreaker._

  val circuitBreaker: ActorSystem[Command] =
    ActorSystem(CircuitBreaker(), "circuit-breaker")
  circuitBreaker ! Send("a")
  circuitBreaker ! Send("b")
  Server.setOk(false)
  circuitBreaker ! Send("c")

}

object Server {
  var ok: Boolean = true

  def setOk(o: Boolean) = {
    Thread.sleep(100)
    ok = o
  }

  def send(message: String): Int = {
    if (ok) {
      println(s"[Server] processing [$message]")
      200
    } else {
      500
    }
  }
}

object CircuitBreaker {

  sealed trait Command
  case class Send(content: String) extends Command
  private case object Close extends Command

  def apply(): Behavior[Command] = closed()

  def closed(): Behavior[Command] = {
    Behaviors.receive { (context, message) =>
      Behaviors.withTimers { timer =>
        message match {
          case Send(content) =>
            val response = Server.send(content)
            if (response == 500) {
              context.log.info(
                s"Server unavailable while sending message: $content")
              timer.startSingleTimer(Close, 3.seconds)
              opened()
            } else {
              Behaviors.same
            }
          case Close =>
            Behaviors.same
        }
      }
    }
  }

  def opened(): Behavior[Command] = {
    Behaviors.receive { (context, message) =>
      message match {
        case Send(content) =>
          context.log.info(
            s"Circuit it's open. Message $content not sent")
          Behaviors.same
        case Close =>
          context.log.info(
            "closing circuit. Ready to send messages to server")
          closed()
      }
    }
  }
}
