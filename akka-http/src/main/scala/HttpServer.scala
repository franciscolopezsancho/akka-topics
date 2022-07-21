package example.http

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model.{ ContentTypes, HttpEntity }
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import scala.io.StdIn

import akka.stream.scaladsl.Source
import akka.http.scaladsl.Http.IncomingConnection
import scala.concurrent.Future

object HttpServer {

  def main(args: Array[String]): Unit = {

    implicit val system = ActorSystem(Behaviors.empty, "simple-api")

    implicit val executionContext = system.executionContext

    val route: Route =
      path("ping") {
        get {
          complete("pong")
        }
      }

    val bindingFuture: Future[ServerBinding] =
      Http().newServerAt("0.0.0.0", 8013).bind(route)

    println(s"server at localhost:8080 \nPress RETURN to stop")
    StdIn.readLine()
    bindingFuture
      .flatMap(_.unbind())
      .onComplete(_ => system.terminate())
  }
}
