import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.BeforeAndAfterAll
import org.scalatest.time.{ Millis, Seconds, Span }

import akka.util.ByteString
import akka.stream.alpakka.s3.scaladsl.S3
import akka.stream.alpakka.s3.MultipartUploadResult
import akka.stream.scaladsl.Source
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors

import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.concurrent.duration._
import scala.util.{ Failure, Success }

class S3Spec
    extends AnyFlatSpec
    with Matchers
    with ScalaFutures
    with BeforeAndAfterAll {

  implicit val system = ActorSystem(Behaviors.empty, "s3test")
  implicit val ec = ExecutionContext.Implicits.global
  implicit val patience =
    PatienceConfig(Span(3, Seconds), Span(150, Millis))

  "alpakka s3" should "be able to upload data to S3" in {

    val result: Future[MultipartUploadResult] = Source
      .single(ByteString("data"))
      .runWith(S3.multipartUpload("franchuelo", "bucketKey"))

    assert(result.futureValue.isInstanceOf[MultipartUploadResult])
    result.map(println)
  }

  import akka.stream.scaladsl.FileIO
  import java.nio.file.FileSystems
  "alpakka s3" should "be able to upload a file to S3" in {

    val fs = FileSystems.getDefault

    val source: Source[ByteString, Future[akka.stream.IOResult]] =
      FileIO.fromPath(
        fs.getPath("./chapter14/src/test/resources/characters.csv"))

    val result: Future[MultipartUploadResult] = source
      .runWith(S3.multipartUpload("franchuelo", "characters.csv"))

    assert(result.futureValue.isInstanceOf[MultipartUploadResult])
  }
}
