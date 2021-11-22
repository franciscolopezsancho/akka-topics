package betting.house.projection

import akka.Done
import akka.actor.typed.ActorSystem
import akka.projection.eventsourced.EventEnvelope
import akka.projection.scaladsl.Handler
import akka.kafka.scaladsl.SendProducer

import org.slf4j.LoggerFactory

import org.apache.kafka.clients.producer.ProducerRecord

import com.google.protobuf.any.{ Any => PbAny }

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
    val record =
      new ProducerRecord(topic, event.marketId, serialize(event))
    val sent = producer.send(record).map { _ =>
      log.debug(s"published event [$event] to topic [$topic]}")
      Done
    }
    sent
  }

  def serialize(event: Market.Event): Array[Byte] = {
    val proto = event match {
      case Market.Closed(marketId, result, _) =>
        projection.proto.MarketClosed(marketId, result)
      case Market.Opened(marketId, _, _) =>
        projection.proto.MarketOpened(marketId)
      case Market.Cancelled(marketId, reason) =>
        projection.proto.MarketCancelled(marketId, reason)
      ///FIXME I'd like to ignore some messages
    }

    PbAny.pack(proto, "market-projection").toByteArray
  }

}
