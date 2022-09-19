package betting.house.projection

import akka.Done
import akka.actor.typed.ActorSystem
import akka.projection.eventsourced.EventEnvelope
import akka.projection.scaladsl.Handler
import akka.kafka.scaladsl.SendProducer

import org.slf4j.LoggerFactory

import org.apache.kafka.clients.producer.ProducerRecord

import com.google.protobuf.any.{ Any => PbAny }
import com.google.protobuf.empty.Empty

import scala.concurrent.{ ExecutionContext, Future }

import example.betting.Market
import betting.house.projection

class MarketProjectionHandler(
    system: ActorSystem[_],
    topic: String,
    producer: SendProducer[String, Array[Byte]])
    extends Handler[EventEnvelope[Market.Event]] {

  val log = LoggerFactory.getLogger(classOf[MarketProjectionHandler])
  implicit val ec = ExecutionContext.global

  override def process(
      envelope: EventEnvelope[Market.Event]): Future[Done] = {
    log.debug(
      s"processing market event [$envelope] to topic [$topic]}")

    val event = envelope.event
    val serializedEvent = serialize(event)
    if (!serializedEvent.isEmpty) {
      val record =
        new ProducerRecord(topic, event.marketId, serializedEvent)
      producer.send(record).map { _ =>
        log.debug(s"published event [$event] to topic [$topic]}")
        Done
      }
    } else {
      Future.successful(Done)
    }
  }

  def serialize(event: Market.Event): Array[Byte] = {
    val proto = event match {
      case Market.Closed(marketId, result, _) =>
        projection.proto.MarketClosed(marketId, result)
      case Market.Opened(marketId, _, _) =>
        projection.proto.MarketOpened(marketId)
      case Market.Cancelled(marketId, reason) =>
        projection.proto.MarketCancelled(marketId, reason)
      case x =>
        log.info(s"ignoring event $x in projection")
        Empty.defaultInstance
    }
    PbAny.pack(proto, "market-projection").toByteArray
  }

}
