package example

import akka.Done
import akka.NotUsed
import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed.ActorRef
import akka.stream.scaladsl.Flow
import akka.stream.scaladsl.Source
import akka.stream.typed.scaladsl.ActorFlow
import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration._

class ActorSpec
    extends ScalaTestWithActorTestKit
    with AnyWordSpecLike
    with Matchers
    with BeforeAndAfter {

  "connecting to an actor from a stream" should {
    "send each element, wait and get back the answer from the actor" ignore {

      val ref = spawn(Cap(0, 3))

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
}
