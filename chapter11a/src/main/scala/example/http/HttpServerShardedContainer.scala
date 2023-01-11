package example.http

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.cluster.sharding.typed.scaladsl.Entity
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import spray.json.DefaultJsonProtocol._
import spray.json.JsonFormat
import spray.json.RootJsonFormat

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.io.StdIn

object HttpServerShardedContainer {

  def main(args: Array[String]): Unit = {
    implicit val system =
      ActorSystem(Behaviors.empty, "simple-api")

    implicit val executionContext = system.executionContext

    implicit val timeout: Timeout = 3.seconds

    implicit val cargoFormat: JsonFormat[Container.Cargo] =
      jsonFormat2(Container.Cargo)
    implicit val cargosFormat: RootJsonFormat[Container.Cargos] =
      jsonFormat1(Container.Cargos)

    val sharding = ClusterSharding(system)

    val shardingRegion =
      sharding.init(Entity(Container.TypeKey)(entityContext =>
        Container(entityContext.entityId)))

    val route: Route =
      path("cargo") {
        concat(
          post {
            parameters( // or
              "entityId"
                .as[String], // entity(as[Container.Cargo]) { cargo =>
              "kind".as[String], //
              "size".as[Int]) { (entityId, kind, size) => //
              val cargo = Container.Cargo(kind, size) //

              shardingRegion ! ShardingEnvelope(
                entityId,
                Container.AddCargo(cargo))

              complete(StatusCodes.Accepted, "Adding Cargo requested")
            }
          },
          get {
            parameters("entityId".as[String]) { entityId =>
              val container =
                sharding.entityRefFor(Container.TypeKey, entityId)

              val response: Future[Container.Cargos] =
                container
                  .ask(Container.GetCargos)
                  .mapTo[Container.Cargos]

              complete(response)
            }
          })
      }

    val bindingFuture: Future[ServerBinding] =
      Http().newServerAt("localhost", 8080).bind(route)

    println(s"server at localhost:8080 \nPress RETURN to stop")
    StdIn.readLine()
    bindingFuture
      .flatMap(_.unbind())
      .onComplete(_ => system.terminate())
  }
}