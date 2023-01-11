package example

import akka.Done
import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.stream.scaladsl.Source
import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import java.util.concurrent.Executors
import scala.collection.StringOps
import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.Promise
import scala.concurrent.duration._

class CPUNonBoundedSpec
    extends ScalaTestWithActorTestKit
    with AnyWordSpecLike
    with Matchers
    with BeforeAndAfter {

  implicit val ec =
    ExecutionContext.fromExecutor(
      Executors.newFixedThreadPool(1000)
    ) //

  "calling to an external service" should {
    "be much faster" ignore {

      val init = System.currentTimeMillis

      val result: Future[Done] =
        Source(1 to 5000)
          .mapAsync(1000)(each => externalFilter(each))
          .map(each => print1000th(each, init))
          .run()

      Await.result(result, 6.seconds)
      assert(result.isCompleted)
    }
  }

  import akka.actor.typed.scaladsl.adapter._ //  provides system.toClassic

  //External Call non CPU bounded
  // can scale as much as the service limitation to scale.
  def externalFilter(number: Int): Future[Int] = {

    val promise = Promise[Int]

    system.toClassic.scheduler.scheduleOnce(1.second) {
      promise.success(
        if (number % 11 == 0) 1
        else number)
    }
    promise.future
  }

  "calling to an external service" should {
    "is slower ordered when duration is uneven" ignore {

      val init = System.currentTimeMillis

      val result: Future[Done] =
        Source(1 to 5000)
          .mapAsync(1000)(each => randomDurationQuery(each.toString))
          .map(each => print1000th(each.toInt, init))
          .run()

      Await.result(result, 33.seconds)
    }
  }

  def print1000th(element: Int, init: Long) {
    if (element % 1000 == 0) {
      println(
        element + "th element processed. Total time elapsed: " + (System.currentTimeMillis - init) / 1000 + "s")
    }
  }

  def randomDurationQuery(doc: String): Future[String] = {

    val promise = Promise[String]

    val duration = scala.util.Random.nextInt(7)

    system.toClassic.scheduler.scheduleOnce(duration.second) {
      promise.success(
        new StringOps(doc)
          .filterNot(_ == 'a') // some masking happens here
      )
    }
    promise.future
  }

  "calling to a fake external service" should {
    "is faster unordered when duration is uneven" ignore {

      val init = System.currentTimeMillis

      val result: Future[Done] =
        Source(1 to 5000)
          .mapAsyncUnordered(1000)(each =>
            randomDurationQuery(each.toString))
          .map(each => print1000th(each.toInt, init))
          .run()

      Await.result(result, 22.seconds)

    }
  }

  import akka.http.scaladsl.Http
  import akka.http.scaladsl.client.RequestBuilding.Get
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import akka.http.scaladsl.unmarshalling.Unmarshal
  import example.validation.HttpServer
  import spray.json.DefaultJsonProtocol._
  import spray.json.RootJsonFormat

  "calling to a real service" should {
    "be handle with throttle and mapAsync" ignore {

      implicit val validatedFormat
          : RootJsonFormat[HttpServer.Validated] =
        jsonFormat1(HttpServer.Validated)

      //akka-http-core/src/main/resources/reference.conf  max-open-requests = 32
      val result: Future[Done] =
        Source(1 to 100)
          .throttle(32, 1.second)
          .mapAsync(100)(each =>
            Http().singleRequest(
              Get("http://localhost:8080/validate?quantity=" + each)))
          .mapAsync(100)(each =>
            Unmarshal(each).to[HttpServer.Validated])
          .map(println)
          .run()

      Await.result(result, 5.seconds)
    }
  }

  import scalikejdbc._
  import config._

  "connecting to a db" should {
    "work" ignore {

      DBs.setup(Symbol("containers"))

      def myGetPersistenceId(seqNum: Int): Option[String] = {
        val name: Option[String] =
          NamedDB(Symbol("containers")).readOnly { implicit session =>
            sql"select persistence_id from event_journal where sequence_number = ${seqNum}"
              .map(rs => rs.string("persistence_id"))
              .first
              .apply()
          }
        name
      }

      //use throttle to send 1000 queries?
      val result = Source(1 to 100)
        .mapAsync(100)(each => Future(myGetPersistenceId(each % 10)))
        .map(println)
        .run()

      Await.result(result, 5.seconds)
      DBs.close(Symbol("containers"))
    }
  }
}
