import akka.Done
import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.actor.typed.javadsl.Behaviors
import akka.stream.SystemMaterializer
import akka.stream.scaladsl.Flow
import akka.stream.scaladsl.Keep
import akka.stream.scaladsl.RunnableGraph
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Source

import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration._

object Main extends App {

  implicit val system = ActorSystem(Behaviors.empty,"runner")
  system
  var fakeDB: List[Int] = List()
  def storeDB(value: Int) =
    fakeDB = fakeDB :+ value

  val producer: Source[Int, NotUsed] = Source(List(1, 2, 3))
  val processor: Flow[Int, Int, NotUsed] =
    Flow[Int].filter(_ % 2 == 0)
  val consumer: Sink[Int, Future[Done]] =
    Sink.foreach(i => storeDB(i))

  val blueprint
  : RunnableGraph[scala.concurrent.Future[akka.Done]] =
  producer.via(processor).toMat(consumer)(Keep.right)

  val future: Future[Done] = blueprint.run()(SystemMaterializer(system).materializer)
  Await.result(future, 1.seconds)
  assert(fakeDB == List(2))
}
