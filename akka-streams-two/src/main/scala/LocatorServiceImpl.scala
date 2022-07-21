package example.locator.grpc

import example.locator.grpc.{ Location, LocatorService }
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import akka.stream.scaladsl.{ Flow, Keep, Sink, Source }
import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import com.google.protobuf.empty.Empty

class LocatorServiceImpl(val init: Int)(
    implicit executionContext: ExecutionContext,
    system: ActorSystem[_])
    extends LocatorService {

  override def follow(in: Empty): Source[Location, NotUsed] = {
    val it = (init to init + 100)
      .map(each => Location(each, -3.701101d))
      .toIterator
    val sou = Source
      .fromIterator(() => it)
      .throttle(1, 1.second)
      .runWith(Sink.asPublisher(fanout = false))
    Source.fromPublisher(sou)

  }

}
