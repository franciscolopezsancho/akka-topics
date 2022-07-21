package betting.house.projection

import org.slf4j.LoggerFactory
import akka.actor.CoordinatedShutdown
import akka.actor.typed.ActorSystem

import akka.cluster.sharding.typed.scaladsl.ShardedDaemonProcess
import akka.cluster.sharding.typed.ShardedDaemonProcessSettings

import akka.projection.{ ProjectionBehavior, ProjectionId }
import akka.projection.scaladsl.{
  AtLeastOnceProjection,
  SourceProvider
}
import akka.projection.eventsourced.EventEnvelope
import akka.projection.eventsourced.scaladsl.EventSourcedProvider
import akka.projection.jdbc.scaladsl.JdbcProjection

import akka.persistence.query.Offset
import akka.persistence.jdbc.query.scaladsl.JdbcReadJournal

import example.repository.scalike.ScalikeJdbcSession

import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.SendProducer

import org.apache.kafka.common.serialization.{
  ByteArraySerializer,
  StringSerializer
}

import example.betting.Market

//Bets grouped per Market and Wallet
object MarketProjection { //BPM

  val logger =
    LoggerFactory.getLogger(MarketProjection + "")

  def init(system: ActorSystem[_]): Unit = {
    val producer = createProducer(system)
    val topic =
      system.settings.config
        .getString("kafka.market-projection.topic")

    ShardedDaemonProcess(system).init(
      name = "MarketProjection",
      Market.tags.size,
      index =>
        ProjectionBehavior(
          createProjection(system, topic, producer, index)),
      ShardedDaemonProcessSettings(system),
      Some(ProjectionBehavior.Stop))
  }

  def createProducer(
      system: ActorSystem[_]): SendProducer[String, Array[Byte]] = {

    val producerSettings =
      ProducerSettings( //the look up on creation at "akka.kafka.producer" in .conf
        system,
        new StringSerializer,
        new ByteArraySerializer)
    val sendProducer = SendProducer(producerSettings)(system)
    CoordinatedShutdown(system).addTask(
      CoordinatedShutdown.PhaseBeforeActorSystemTerminate,
      "closing send producer") { () =>
      sendProducer.close()
    } //otherwise trying to restart the application you would probably get [2021-11-22 14:56:32,024] [WARN] [org.apache.kafka.common.utils.AppInfoParser] [] [betting-house-akka.kafka.default-dispatcher-20] - Error registering AppInfo mbean javax.management.InstanceAlreadyExistsException: kafka.producer:type=app-info,id=producer-
    sendProducer
  }

  private def createProjection(
      system: ActorSystem[_],
      topic: String,
      producer: SendProducer[String, Array[Byte]],
      index: Int)
      : AtLeastOnceProjection[Offset, EventEnvelope[Market.Event]] = {
    val tag = Market.tags(index)
    val sourceProvider
        : SourceProvider[Offset, EventEnvelope[Market.Event]] =
      EventSourcedProvider.eventsByTag[Market.Event](
        system = system,
        readJournalPluginId = JdbcReadJournal.Identifier,
        tag = tag)

    JdbcProjection.atLeastOnceAsync(
      projectionId = ProjectionId("MarketProjection", tag),
      sourceProvider = sourceProvider,
      handler =
        () => new MarketProjectionHandler(system, topic, producer),
      sessionFactory = () => new ScalikeJdbcSession())(system)
  }

}
