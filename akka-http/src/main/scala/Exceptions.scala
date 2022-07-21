package example.http

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.StatusCodes.Accepted
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{
  ExceptionHandler,
  MissingCookieRejection,
  Route,
  StandardRoute
}
import scala.io.StdIn
import scala.util.Random

object HttpServerExceptions {

  def main(args: Array[String]): Unit = {

    implicit val system = ActorSystem(Behaviors.empty, "simple-api")

    implicit val executionContext = system.executionContext

    def exceptionHandler = ExceptionHandler {
      case _: ArithmeticException =>
        extractUri { uri =>
          complete(
            HttpEntity(
              ContentTypes.`application/json`,
              s"sorry something went wrong with $uri")
          ) //there are many other parameters like headers or httpprotocol where you can choose http 1.0 or http 1.1
        }
    }

    val route: Route =
      handleExceptions(exceptionHandler) {
        path("imfeelinglucky") {
          get {
            complete(
              HttpEntity(
                ContentTypes.`application/json`,
                "exactly the site you wanted " + (1 / Random.nextInt(2))))
          }
        }
      }

    val bindingFuture =
      Http().newServerAt("localhost", 8080).bind(route)

    println(s"server at localhost:8080 \nPress RETURN to stop")
    StdIn.readLine()
    bindingFuture
      .flatMap(_.unbind())
      .onComplete(_ => system.terminate())
  }
}
