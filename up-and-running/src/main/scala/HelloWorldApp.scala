package com.manning

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.ActorSystem

object HelloWorldApp extends App {

  val guardian: ActorSystem[String] =
    ActorSystem(HelloWorld(), "HelloWorldExampleApp")
  guardian ! "hello, world"
  guardian ! "hello, again"

}

object HelloWorld {

  def apply(): Behaviors.Receive[String] =
    Behaviors.receive { (context, message) =>
      context.log.info(s"received message '$message'")
      Behaviors.same
    }

}
