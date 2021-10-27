import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors

import akka.kafka.{ ConsumerSettings, Subscriptions }
import akka.kafka.scaladsl.Consumer

import akka.stream.scaladsl.Sink

import org.apache.kafka.clients.consumer.{
  ConsumerConfig,
  ConsumerRecord
}
import org.apache.kafka.common.serialization.StringDeserializer

import scala.io.StdIn

object ConsumerOne {

  def main(args: Array[String]) = {

    implicit val system = ActorSystem(Behaviors.empty, "consumerOne")

    val config =
      system.settings.config.getConfig("akka.kafka.consumer")

    val consumerSettings: ConsumerSettings[String, String] =
      ConsumerSettings(
        config,
        new StringDeserializer(),
        new StringDeserializer())
        .withBootstrapServers("127.0.0.1:9092")
        .withGroupId("group01")
        // .withProperty(
        //   ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
        //   "earliest")
        .withProperty(
          ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,
          "true")

    Consumer
      .plainSource(consumerSettings, Subscriptions.topics("test"))
      .map { msg: ConsumerRecord[String, String] =>
        println(
          s"key = ${msg.key}, value = ${msg.value}, offset = ${msg.value}")
      //side effect
      }
      .runWith(Sink.ignore)

    StdIn.readLine("Consumer started \n Press ENTER to stop")
    system.terminate()
  }
  //kafkacat -P -b 127.0.0.1:9092 -t test
}
