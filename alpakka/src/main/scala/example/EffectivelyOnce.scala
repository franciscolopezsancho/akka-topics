package com.example

import org.apache.kafka.common.serialization.{
  StringDeserializer,
  StringSerializer
}
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.consumer.{ ConsumerConfig }

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors

import akka.kafka.{
  ConsumerSettings,
  ProducerSettings,
  Subscriptions
}
import akka.kafka.ConsumerMessage.TransactionalMessage
import akka.kafka.ProducerMessage
import akka.kafka.scaladsl.{ Consumer, Transactional }

import akka.Done

import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.concurrent.duration._
import scala.io.StdIn

object EffectivelyOnce {

  implicit val system = ActorSystem(Behaviors.empty, "producerOne")

  implicit val ec = ExecutionContext.Implicits.global

  def main(args: Array[String]) = {
    require(
      args.length == 1,
      "one parameter is required to set the transaction's id")

    val transactionalId = args(0)

    val bootstrapServers = "127.0.0.1:9092"

    val consumerConfig =
      system.settings.config.getConfig("akka.kafka.consumer")

    val consumerSettings: ConsumerSettings[String, String] =
      ConsumerSettings(
        consumerConfig,
        new StringDeserializer(),
        new StringDeserializer())
        .withBootstrapServers(bootstrapServers)
        .withGroupId("group01")
        .withProperty(
          ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
          "earliest")

    val producerConfig =
      system.settings.config.getConfig("akka.kafka.producer")

    val producerSettings = ProducerSettings(
      producerConfig,
      new StringSerializer(),
      new StringSerializer())
      .withBootstrapServers(bootstrapServers)

    val drainingControl: Consumer.DrainingControl[_] =
      Transactional
        .source(
          consumerSettings,
          Subscriptions.topics("test5")
        ) //we don't need commiter settings
        .map { msg: TransactionalMessage[String, String] =>
          ProducerMessage.single(
            new ProducerRecord[String, String](
              "test6",
              msg.record.key,
              msg.record.value),
            msg.partitionOffset)
        }
        .toMat(Transactional.sink(producerSettings, transactionalId))(
          Consumer.DrainingControl.apply)
        .run()

    StdIn.readLine("Consumer started \n Press ENTER to stop")
    val future = drainingControl.drainAndShutdown
    future.onComplete(_ => system.terminate)

  }
}
