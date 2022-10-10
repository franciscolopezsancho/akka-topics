import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.BeforeAndAfterAll

import akka.stream.scaladsl.{ Sink, Source }

import akka.stream.alpakka.csv.scaladsl.CsvParsing

import akka.util.ByteString

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors

import scala.concurrent.duration._
import scala.concurrent.{ Await, ExecutionContext, Future }

import scala.util.Success

class CSVSpec
    extends AnyFlatSpec
    with Matchers
    with ScalaFutures
    with BeforeAndAfterAll {

  implicit val system =
    ActorSystem(Behaviors.empty, "csvExamples")

  implicit val ec: ExecutionContext =
    ExecutionContext.Implicits.global //not very recommendable. Explain

  //or Executors.newSingleThreadExecutor

  "A CSV Source" should "be readable" in {

    val result: Future[Seq[List[ByteString]]] =
      Source
        .single(ByteString("Alice,Pleasence,19\nBob,Marley,30\n"))
        .via(CsvParsing.lineScanner())
        .runWith(Sink.seq)

    result.onComplete {
      case Success(lines) =>
        lines.head should be(
          List(
            ByteString("Alice"),
            ByteString("Pleasence"),
            ByteString("19")))

        lines(1) should be(
          List(
            ByteString("Bob"),
            ByteString("Marley"),
            ByteString("30")))
    }
  }

  "A CSV" should "be readable in shorter test" in {

    val result: Future[Seq[List[ByteString]]] =
      Source
        .single(ByteString("Alice,Pleasence,19\nBob,Marley,30\n"))
        .via(CsvParsing.lineScanner())
        .runWith(Sink.seq)

    val lines = result.futureValue
    lines.head should be(
      List(
        ByteString("Alice"),
        ByteString("Pleasence"),
        ByteString("19")))

    lines(1) should be(
      List(ByteString("Bob"), ByteString("Marley"), ByteString("30")))

  }

  override protected def afterAll(): Unit = {
    system.terminate()
  }

  import akka.stream.alpakka.csv.scaladsl.CsvToMap

  "A CSV with NO headers" should "be able to get into a Map[String,BytString]" in {
    val result: Future[Seq[Map[String, ByteString]]] =
      Source
        .single(ByteString("Alice,Pleasence,19\nBob,Marley,30\n"))
        .via(CsvParsing.lineScanner())
        .via(CsvToMap.withHeaders("name", "surname", "age"))
        .runWith(Sink.seq)

    val lines = result.futureValue
    lines.head should be(
      Map(
        "name" -> ByteString("Alice"),
        "surname" -> ByteString("Pleasence"),
        "age" -> ByteString("19")))
  }

  "A CSV with headers" should "be able to get into a Map[String,String]" in {
    val result: Future[Seq[Map[String, String]]] =
      Source
        .single(ByteString(
          "name,surname,age\nAlice,Pleasence,19\nBob,Marley,30\n"))
        .via(CsvParsing.lineScanner())
        .via(CsvToMap.toMapAsStrings())
        .runWith(Sink.seq)

    val lines = result.futureValue
    lines.head should be(
      Map("name" -> "Alice", "surname" -> "Pleasence", "age" -> "19"))
  }

  import akka.stream.scaladsl.FileIO
  import java.nio.file.FileSystems
  import akka.NotUsed
  import org.scalatest.time.{ Millis, Seconds, Span }

  implicit val patience = PatienceConfig(
    timeout = Span(9, Seconds),
    interval = Span(150, Millis))

  "A file" should "be able to be read" in {
    val fs = FileSystems.getDefault
    val linesSource
        : Source[ByteString, Future[akka.stream.IOResult]] =
      FileIO.fromPath(
        fs.getPath("./chapter14/src/test/resources/characters.csv"))

    val result = linesSource
      .via(CsvParsing.lineScanner())
      .via(CsvToMap.toMapAsStrings())
      .runWith(Sink.seq)

    val lines = result.futureValue
    lines.head should be(
      Map("name" -> "Alice", "surname" -> "Pleasence", "age" -> "19"))
  }

//example of cleanse
//https://github.com/akka/alpakka/blob/v1.0.0/doc-examples/src/main/scala/csvsamples/FetchHttpEvery30SecondsAndConvertCsvToJsonToKafka.scala#L8-L76

  import akka.stream.alpakka.csv.scaladsl.CsvFormatting
  import akka.stream.scaladsl.{ Flow, FlowOps }

  //A FileWatcher that triggers this main
  "A CSV file" should "be able to be read and write another CSV file" in {
    val fs = FileSystems.getDefault
    val source: Source[ByteString, Future[akka.stream.IOResult]] =
      FileIO.fromPath(
        fs.getPath("./chapter14/src/test/resources/characters.csv"))

    val filterAndGetValues
        : Flow[Map[String, String], Seq[String], NotUsed] =
      Flow[Map[String, String]]
        .filter(eachMap => eachMap("age").toInt > 18)
        .map(
          eachMap => eachMap.values.toSeq
        ) //to make it from collection.Iterable to collection.immutable.Iterable

    val result = source
      .via(CsvParsing.lineScanner())
      .via(CsvToMap.toMapAsStrings())
      .via(filterAndGetValues)
      .via(
        CsvFormatting.format()
      ) //Flow[T, ByteString, NotUsed] T < Colletion[String]
      .runWith(FileIO.toPath(
        fs.getPath("./chapter14/src/test/resources/result.csv")))

  }

}
