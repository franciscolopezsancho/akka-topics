package example.locator.grpc

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import scala.concurrent.{ ExecutionContext, Future }
import akka.http.scaladsl.{ Http, HttpConnectionContext }
import akka.http.scaladsl.model.{ HttpRequest, HttpResponse }

import scala.io.StdIn

object LocatorServer {

  def main(args: Array[String]): Unit = {

    val init = args(0).toInt

    implicit val system =
      ActorSystem(Behaviors.empty, "LocatorServer")

    implicit val ec: ExecutionContext = system.executionContext

    val service: HttpRequest => Future[HttpResponse] =
      LocatorServiceHandler.withServerReflection(
        new LocatorServiceImpl(init))

    val bindingFuture: Future[Http.ServerBinding] =
      Http().newServerAt("0.0.0.0", 8080).bind(service)

    println(s"server at localhost:8080 \nPress RETURN to stop")
    StdIn.readLine()
    bindingFuture
      .flatMap(_.unbind())
      .onComplete(_ => system.terminate())
  }
}
