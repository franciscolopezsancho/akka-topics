// import org.scalatest.flatspec.AnyFlatSpec
// import org.scalatest.matchers.should.Matchers
// import org.scalatest.BeforeAndAfterAll

// import akka.actor.typed.ActorSystem
// import akka.actor.typed.scaladsl.Behaviors

// import akka.kafka.{ ConsumerSettings, Subscriptions }
// import akka.kafka.scaladsl.Consumer

// import akka.stream.scaladsl.Sink

// import org.apache.kafka.clients.consumer.ConsumerConfig
// import org.apache.kafka.common.serialization.{
//   ByteArrayDeserializer,
//   StringDeserializer
// }

// class KafkaSpec
//     extends AnyFlatSpec
//     with Matchers
//     with BeforeAndAfterAll {

//   implicit val system = ActorSystem(Behaviors.empty, "kafka")

//   "Alpakka Kafka" should "be able to read from Kafka topic" in {

//     val config =
//       system.settings.config.getConfig("akka.kafka.consumer")

//     val consumerSettings =
//       ConsumerSettings(
//         config,
//         new StringDeserializer(),
//         new ByteArrayDeserializer())
//         .withBootstrapServers("localhost:9092")
//         .withGroupId("test1")
//         .withProperty(
//           ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
//           "earliest")

//     Consumer
//       .plainSource(
//         consumerSettings,
//         Subscriptions.topics("topictest"))
//       .map(println)
//       .runWith(Sink.ignore)

//   }

//   override def afterAll() {
//     system.terminate()
//   }
// }
