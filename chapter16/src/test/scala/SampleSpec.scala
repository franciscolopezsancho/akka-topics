package example.streams.two

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.matchers.should.Matchers
import org.scalatest.BeforeAndAfter

import scala.concurrent.{ Await, ExecutionContext, Future, Promise }
import scala.concurrent.duration._
import scala.collection.StringOps
import java.util.concurrent.Executors
import akka.stream.scaladsl.{ Flow, Keep, Sink, Source }
import akka.Done
import akka.NotUsed

class SampleSpec
    extends ScalaTestWithActorTestKit
    with AnyWordSpecLike
    with Matchers
    with BeforeAndAfter {

// https://jessitron.com/2014/01/29/choosing-an-executorservice/
  implicit val ec =
    ExecutionContext.fromExecutor(Executors.newFixedThreadPool(1000))

  "calling local process" should {
    "eventually return" ignore {

      val result: Future[Done] =
        Source(1 to 10)
          .map(each => parsingDoc(each.toString))
          .run()

      Await.result(result, 30.seconds)
    }
  }

  "calling to an external service" should {
    "eventually return 3 times faster" ignore {

      val result: Future[Done] =
        Source(1 to 10)
          .mapAsync(10)(each => Future(parsingDoc(each.toString)))
          .run()

      Await.result(result, 10.seconds)

    }
  }

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

  def printTh(th: Int)(element: Int) {
    if (element % th == 0) {
      println(element)
    }
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

  import akka.http.scaladsl.client.RequestBuilding.Get
  import akka.http.scaladsl.Http
  import akka.http.scaladsl.unmarshalling.Unmarshal
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import spray.json.DefaultJsonProtocol._
  import spray.json.RootJsonFormat

  import example.validation.HttpServer
  "calling to a real service" should {
    "work" ignore {

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

  import scalikejdbc._, config._

  "connecting to a db" should {
    "work" ignore {

      DBs.setup('containers)

      def myGetPersistenceId(id: Int): Option[String] = {
        val name: Option[String] = NamedDB('containers).readOnly {
          implicit session =>
            sql"select persistence_id from event_journal where sequence_number = ${id}"
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
      DBs.close('containers)
    }
  }

  import akka.actor.typed.{ ActorRef, Behavior }
  import akka.actor.typed.scaladsl.Behaviors

  object Cap {

    case class Increment(increment: Int, replyTo: ActorRef[Int])

    def apply(current: Int, max: Int): Behavior[Increment] = {
      Behaviors.receiveMessage { message =>
        message match {
          case Increment(increment, replyTo) =>
            if (current + increment > max) {
              replyTo ! current
              Behaviors.same
            } else {
              replyTo ! current + increment
              apply(current + increment, max)
            }
        }
      }
    }
  }

  import akka.stream.typed.scaladsl.ActorFlow

  "connecting to an actor from a stream" should {
    "send each element, wait and get back the answer from the actor" ignore {

      val ref = spawn(Cap(0, 3))

      //TODO we can add throttle and timeout
      val askFlow: Flow[Int, Int, NotUsed] =
        ActorFlow.ask(ref)((elem: Int, replyTo: ActorRef[Int]) =>
          Cap.Increment(elem, replyTo))

      //val sugaryAskFlow = ActorFlow.ask(ref)(Cap.Increment)

      val result: Future[Done] = Source(1 to 10)
        .via(askFlow)
        .map(println)
        .run()

      Await.result(result, 1.seconds)
    }
  }

  "connecting to an actor async" should {
    "send an element from the stream and get back an answer from the actor" ignore {

      val ref2 = spawn(Cap(0, 1000))

      //TODO we can add throttle and timeout
      val asyncAskFlow =
        ActorFlow.ask(100)(ref2)(
          (elem: Int, replyTo: ActorRef[Int]) =>
            Cap.Increment(elem, replyTo))

      val result2: Future[Done] = Source(1 to 1000000)
        .via(asyncAskFlow)
        .run()

      Await.result(result2, 10.seconds)

    }
  }

  import scala.util.Random

  "an Exception" should {
    "stop the stream and log 'tried riskyHandle'" ignore {
      def riskyHandler(elem: Int): Int =
        100 / elem

      val result =
        Source(-1 to 1)
          .map(riskyHandler)
          .log("tried riskyHandler")
          .map(println)
          .run()

      Await.result(result, 1.seconds)
    }
  }

  "an Exception" should {
    "be possible to overcome by a last message" ignore {

      def riskyHandler(elem: Int): Int =
        100 / elem

      val result =
        Source(-1 to 1)
          .map(riskyHandler)
          .log("tried riskyHandler")
          .recover {
            case e: ArithmeticException => 0
          }
          .map(println)
          .run()

      Await.result(result, 3.seconds)
    }
  }

  import akka.stream.Supervision
  import akka.stream.ActorAttributes

  val decider: Supervision.Decider = {
    case _: ArithmeticException => Supervision.Resume
    case _                      => Supervision.Stop
  }

  "an Exception" should {
    "be possible to overcome ArithmeticException" ignore {

      def riskyHandler(elem: Int): Int = {
        if (elem > 2)
          throw new IllegalArgumentException(
            "no higher than 2 please")
        100 / elem
      }

      val result =
        Source(-1 to 3)
          .map(riskyHandler)
          .log("riskyHandler")
          .map(println)
          .withAttributes(
            ActorAttributes.supervisionStrategy(decider))
          .run()

      Await.result(result, 1.seconds)
    }
  }

  val decider2: Supervision.Decider = {
    case _: ArithmeticException => Supervision.Resume
    case _                      => Supervision.Stop
  }

  "an Exception" should {
    "be possible to overcome by continuing and restarting" ignore {

      def riskyHandler(elem: Int): Int =
        100 / elem

      var state = 0
      val flow = Flow[Int]
        .map { each => state += 1; each }
        .map(riskyHandler)
        .map(println)

      val result =
        Source(-1 to 1)
          .via(flow)
          .withAttributes(
            ActorAttributes.supervisionStrategy(decider))
          .run()

      Await.result(result, 1.seconds)
      println(state)
    }
  }

  "The invalid results" should {
    "be diverted to another Sink as soon as possible" ignore {

      case class Result(
          value: Int,
          isValid: Boolean,
          message: String = "")

      def properHandler(elem: Int): Result =
        try {
          Result((100 / elem), true)
        } catch {
          case ex: ArithmeticException =>
            Result(
              elem,
              false,
              s"failed processing [$elem] with $ex.")
        }

      val sink1 = Sink.foreach { each: Result =>
        println(
          s"${each.value} has been diverted. Caused by: ${each.message}")
      }

      val sink2 = Sink.foreach { each: Result =>
        println(s"${each.value} has been successfully processed")
      }

      Source(-1 to 1)
        .map(properHandler)
        .divertTo(sink1, !_.isValid)
        //some more processing
        .to(sink2)
        .run()

    }
  }

  import akka.stream.scaladsl.RestartSource
  import akka.stream.RestartSettings
  import example.locator.grpc.{
    Location,
    LocatorService,
    LocatorServiceClient
  }
  import akka.grpc.GrpcClientSettings
  import com.google.protobuf.empty.Empty

  "A Source" should {
    "be able to keep consuming from a failed and restored service" ignore {

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

  import akka.stream.BoundedSourceQueue

  "Source queue" should {
    "allow adding elements to a stream" ignore {

      val bufferSize = 10

      val queue: BoundedSourceQueue[Int] = Source
        .queue[Int](
          bufferSize
        ) // elements will may be dropped when the bufferSize is overflown
        .map(x => 2 * x)
        .toMat(Sink.foreach(x => println(s"processed $x")))(Keep.left)
        .run()

      (1 to 10).map(x => queue.offer(x))
      Thread.sleep(1000)
    }
  }

  import akka.stream.{ OverflowStrategy, QueueOfferResult }
  import akka.stream.scaladsl.SourceQueue

  "Source queue" should {
    "allow to configure its overflow strategy" ignore {

      val bufferSize = 4

      val queue: SourceQueue[Int] = Source
        .queue[Int](bufferSize, OverflowStrategy.dropHead)
        .throttle(1, 100.millis)
        .map(x => 2 * x)
        .toMat(Sink.foreach(x => println(s"PROCESSED $x")))(Keep.left)
        .run()

      (1 to 10).map(x => queue.offer(x))

      Thread.sleep(1000)
    }
  }

  "Source queue" should {
    "allow to configure its overflow strategy and print QueueOfferResult " ignore {

      val bufferSize = 4

      val queue: SourceQueue[Int] = Source
        .queue[Int](bufferSize, OverflowStrategy.dropNew)
        .throttle(1, 100.millis)
        .map(x => 2 * x)
        .toMat(Sink.foreach(x => println(s"PROCESSED $x")))(Keep.left)
        .run()

      (1 to 10)
        .map(x => {
          queue.offer(x).map {
            case QueueOfferResult.Enqueued => println(s"enqueued $x")
            case QueueOfferResult.Dropped  => println(s"dropped $x")
          }
        })

      Thread.sleep(1000)
    }
  }

  //Internal call, CPU bounded
  //80000 takes about 1-3 seconds one call
  // load is cumulative and the more threads we run
  // the more they have to share the CPU
  def parsingDoc(doc: String): String = {
    val init = System.currentTimeMillis
    factorial(new java.math.BigInteger("80000"))
    println(
      s"${((System.currentTimeMillis - init).toDouble / 1000).toDouble}" + "s")
    doc
  }

  import akka.actor.typed.scaladsl.adapter._
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

  def randomDurationQuery(doc: String): Future[String] = {

    val promise = Promise[String]

    val duration = scala.util.Random.nextInt(7)

    system.toClassic.scheduler.scheduleOnce(duration.second) {
      promise.success(
        new StringOps(doc).filterNot(_ == 'a')
      ) // some masking happens here
    }
    promise.future
  }

  import scala.annotation.tailrec
  def factorial(x: BigInt): BigInt = {
    @tailrec
    def loop(x: BigInt, acc: BigInt = 1): BigInt = {
      if (x <= 1) acc
      else loop(x - 1, x * acc)
    }
    loop(x)
  }
  //

}
