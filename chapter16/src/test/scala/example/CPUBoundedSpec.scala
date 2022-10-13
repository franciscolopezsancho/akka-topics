package example

import akka.Done
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

class CPUBoundedSpec
    extends ScalaTestWithActorTestKit
    with AnyWordSpecLike
    with Matchers
    with BeforeAndAfter {

  "calling to a local function sync" should {
    "eventually return" ignore {

      val result: Future[Done] =
        Source(1 to 10)
          .map(each => parsingDoc(each.toString))
          .run()

      Await.result(result, 30.seconds)
    }
  }

  implicit val ec =
    ExecutionContext.fromExecutor(Executors.newFixedThreadPool(12))

  "calling to an local function async" should {
    "eventually return 3 times faster" ignore {

      val result: Future[Done] =
        Source(1 to 10)
          .mapAsync(10)(each => Future(parsingDoc(each.toString)))
          .run()

      Await.result(result, 10.seconds)

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

  import scala.annotation.tailrec
  def factorial(x: BigInt): BigInt = {
    @tailrec
    def loop(x: BigInt, acc: BigInt = 1): BigInt = {
      if (x <= 1) acc
      else loop(x - 1, x * acc)
    }
    loop(x)
  }
}
