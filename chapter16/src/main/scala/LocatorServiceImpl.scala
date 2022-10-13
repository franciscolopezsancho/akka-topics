package example.locator.grpc

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import akka.stream.scaladsl.{ Sink, Source }
import akka.NotUsed
import akka.actor.typed.ActorSystem
import com.google.protobuf.empty.Empty

class LocatorServiceImpl(val init: Int)(
    implicit executionContext: ExecutionContext,
    system: ActorSystem[_])
    extends LocatorService {

  override def follow(in: Empty): Source[Location, NotUsed] = {
    val it = (init to init + 100)
      .map(each => Location(each, -3.701101d))
      .iterator
    val sou = Source
      .fromIterator(() => it)
      .throttle(1, 1.second)
      .runWith(Sink.asPublisher(fanout = false))
    Source.fromPublisher(sou)
  }
}
