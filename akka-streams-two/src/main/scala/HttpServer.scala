package example.streams.two

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

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import spray.json.DefaultJsonProtocol._

object HttpServer {

  case class Validated(accepted: Boolean)

  def main(args: Array[String]): Unit = {

    implicit val system = ActorSystem(Behaviors.empty, "simple-api")

    implicit val executionContext = system.executionContext

    implicit val validatedFormat = jsonFormat1(Validated)

    val route: Route =
      path("validate") {
        get {
          parameters("quantity".as[Int]) { message =>
            val accepted =
              if (message > 10)
                Validated(false)
              else Validated(true)
            complete(accepted)
          }
        }
      }

    val bindingFuture: Future[ServerBinding] =
      Http().newServerAt("0.0.0.0", 8080).bind(route)

    println(s"server at localhost:8080 \nPress RETURN to stop")
    StdIn.readLine()
    bindingFuture
      .flatMap(_.unbind())
      .onComplete(_ => system.terminate())
  }
}
