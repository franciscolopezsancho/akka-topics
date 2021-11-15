import akka.actor.typed.{ ActorRef, ActorSystem }
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ Behavior, PostStop }
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import scala.io.StdIn
import scala.concurrent.Future
import scala.concurrent.duration._
import akka.util.Timeout
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.scaladsl.AskPattern.schedulerFromActorSystem
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import spray.json.DefaultJsonProtocol._
import spray.json.{ JsonFormat, RootJsonFormat }

object ContainerAppHttpServer {

  def main(args: Array[String]): Unit = {
    implicit val container = ActorSystem(Container(), "simple-api")

    implicit val executionContext = container.executionContext

    implicit val timeout: Timeout = 3.seconds

    implicit val cargoFormat: JsonFormat[Container.Cargo] =
      jsonFormat2(Container.Cargo)
    implicit val cargosFormat: RootJsonFormat[Container.Cargos] =
      jsonFormat1(Container.Cargos)

    val route: Route =
      path("cargo") {
        concat(
          post {
            //or  entity(as[Container.Cargo]) { cargo =>
            parameters("kind".as[String], "size".as[Int]) {
              (kind, size) =>
                val cargo = Container.Cargo(kind, size)
                container ! Container.AddCargo(cargo)
                complete(
                  StatusCodes.Accepted,
                  "Adding Cargo requested")
            }
          },
          get {
            val response: Future[Container.Cargos] =
              container
                .ask(Container.GetCargos)
                .mapTo[Container.Cargos]
            complete(response)
          })
      }

    val bindingFuture: Future[ServerBinding] =
      Http().newServerAt("0.0.0.0", 8080).bind(route)

    println(s"server at localhost:8080 \nPress RETURN to stop")
    StdIn.readLine()
    bindingFuture
      .flatMap(_.unbind())
      .onComplete(_ => container.terminate())
  }
}

import akka.actor.typed.{ ActorRef, Behavior }
import akka.actor.typed.scaladsl.Behaviors

object Container {

  case class Cargo(kind: String, size: Int)
  case class Cargos(cargos: List[Cargo])

  sealed trait Command
  case class AddCargo(cargo: Cargo) extends Command
  case class GetCargos(replyTo: ActorRef[Cargos]) extends Command

  def apply(cargos: List[Cargo] = Nil): Behavior[Command] = {
    Behaviors.receive { (context, message) =>
      message match {
        case AddCargo(cargo) =>
          println(s"adding cargo $cargo")
          apply(cargo +: cargos)
        case GetCargos(replyTo) =>
          replyTo ! Cargos(cargos)
          Behaviors.same
      }
    }
  }
}
