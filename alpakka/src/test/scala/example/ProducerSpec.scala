import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.BeforeAndAfterAll

import org.apache.kafka.common.serialization.StringSerializer
import org.apache.kafka.clients.producer.ProducerRecord

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors

import akka.kafka.{ ProducerMessage, ProducerSettings }
import akka.kafka.scaladsl.Producer

import akka.stream.scaladsl.Source

import akka.Done

import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._

class ProducerSpec
    extends AnyFlatSpec
    with Matchers
    with BeforeAndAfterAll {

  implicit val system = ActorSystem(Behaviors.empty, "producerOne")

  "a producer" should "produce" in {

    val config =
      system.settings.config.getConfig("akka.kafka.producer")

    val producerSettings = ProducerSettings(
      config,
      new StringSerializer(),
      new StringSerializer()).withBootstrapServers("127.0.0.1:9092")

    val done: Future[Done] = Source(1 to 10)
      .map(_.toString)
      .map(
        elem => new ProducerRecord[String, String]("test", elem)
      ) //we can also pass partition and key
      .runWith(Producer.plainSink(producerSettings))

    Await.ready(done, 3.second)

  }

  "a flexiflow" should "conditionally commit to Kafka en pass downstream "

  override def afterAll() =
    system.terminate()
}
