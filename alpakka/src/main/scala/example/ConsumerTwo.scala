import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors

import akka.kafka.{ ConsumerSettings, Subscriptions }
import akka.kafka.scaladsl.Consumer

import akka.stream.scaladsl.Sink

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer

import scala.io.StdIn

import akka.kafka.scaladsl.Committer
import akka.kafka.scaladsl.Consumer.DrainingControl
import akka.kafka.CommitterSettings
import akka.kafka.ConsumerMessage.CommittableMessage

object ConsumerTwo {

  def main(args: Array[String]) = {

    implicit val system = ActorSystem(Behaviors.empty, "consumerOne")

    implicit val ec = system.executionContext //improve

    val config =
      system.settings.config.getConfig("akka.kafka.consumer")

    val consumerSettings: ConsumerSettings[String, String] =
      ConsumerSettings(
        config,
        new StringDeserializer(),
        new StringDeserializer())
        .withBootstrapServers("127.0.0.1:9092")
        .withGroupId("group02")

    val committerSettings = CommitterSettings(system)

    val drainingControl: DrainingControl[_] = Consumer
      .committableSource(
        consumerSettings,
        Subscriptions.topics("test"))
      .map { msg: CommittableMessage[String, String] =>
        println(
          s"${msg.record.key} => ${msg.record.value.toUpperCase}")
        msg.committableOffset
      }
      .via(Committer.flow(committerSettings.withMaxBatch(100)))
      .toMat(Sink.seq)(DrainingControl.apply)
      .run()

    StdIn.readLine("Consumer started \n Press ENTER to stop")
    val future = drainingControl.drainAndShutdown
    future.onComplete(_ => system.terminate)
  }
  //kafkacat -P -b 127.0.0.1:9092 -t test
}
