package example

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.stream.scaladsl.Flow
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Source
import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.Await
import scala.concurrent.duration._

class ExceptionsSpec
    extends ScalaTestWithActorTestKit
    with AnyWordSpecLike
    with Matchers
    with BeforeAndAfter {

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

  import akka.stream.ActorAttributes
  import akka.stream.Supervision

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

      final case class Result(
          value: Int,
          isValid: Boolean,
          message: String = "")

      def properHandler(elem: Int): Result =
        try {
          Result(100 / elem, true)
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
}
