package example

import akka.NotUsed
import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.stream.scaladsl.Source
import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import java.util.concurrent.Executors
import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration._

import akka.grpc.GrpcClientSettings
import akka.stream.RestartSettings
import akka.stream.scaladsl.RestartSource
import com.google.protobuf.empty.Empty
import example.locator.grpc.Location
import example.locator.grpc.LocatorService
import example.locator.grpc.LocatorServiceClient

class LocatorServiceSpec
    extends ScalaTestWithActorTestKit
    with AnyWordSpecLike
    with Matchers
    with BeforeAndAfter {

  "A Source" should {
    "be able to keep consuming from a failed and restored service" ignore {

      implicit val ec = ExecutionContext.fromExecutor(
        Executors.newFixedThreadPool(12))

      val clientSettings =
        GrpcClientSettings
          .connectToServiceAt("127.0.0.1", 8080)
          .withTls(false)

      val client: LocatorService =
        LocatorServiceClient(clientSettings)

      val restartSettings = RestartSettings(
        minBackoff = 1.seconds,
        maxBackoff = 3.seconds,
        randomFactor = 0.2).withMaxRestarts(30, 3.minutes)

      val restartSource = RestartSource.withBackoff(restartSettings) {
        () =>
          Source.futureSource {
            val responseStream: Source[Location, NotUsed] =
              client.follow(Empty.defaultInstance)
            Future(responseStream)
          }
      }

      val result =
        restartSource
          .map(println)
          .run()

      Await.result(result, 90.seconds)
    }
  }
}
