package example

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.stream.scaladsl.Keep
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Source
import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class QueueSpec
    extends ScalaTestWithActorTestKit
    with AnyWordSpecLike
    with Matchers
    with BeforeAndAfter {

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

  import akka.stream.OverflowStrategy
  import akka.stream.QueueOfferResult
  import akka.stream.scaladsl.SourceQueue

  "Source queue" should {
    "allow to configure its overflow strategy" ignore {

      val bufferSize = 4

      val queue: SourceQueue[Int] = Source
        .queue[Int](bufferSize, OverflowStrategy.dropHead)
        .throttle(1, 100.millis)
        .map(x => 2 * x)
        .toMat(Sink.foreach(x => println(s"processed $x")))(Keep.left)
        .run()

      (1 to 10).map(x => queue.offer(x))

      Thread.sleep(1000)
    }
  }

  "Source queue" should {
    "allow to configure its overflow strategy and print QueueOfferResult " ignore {

      implicit val ec = ExecutionContext.fromExecutor(
        Executors.newFixedThreadPool(12))

      val bufferSize = 4

      val queue: SourceQueue[Int] = Source
        .queue[Int](bufferSize, OverflowStrategy.dropNew)
        .throttle(1, 100.millis)
        .map(x => 2 * x)
        .toMat(Sink.foreach(x => println(s"processed $x")))(Keep.left)
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
}
