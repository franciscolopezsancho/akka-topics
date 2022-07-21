package routers

import akka.actor.testkit.typed.scaladsl.{
  LogCapturing,
  ScalaTestWithActorTestKit,
  TestProbe
}

import akka.actor.typed.scaladsl.{
  Behaviors,
  GroupRouter,
  PoolRouter,
  Routers
}
import akka.actor.typed.{ ActorRef, Behavior }
import akka.actor.typed.receptionist.{ Receptionist, ServiceKey }

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class RoutingExamplesSpec
    extends ScalaTestWithActorTestKit
    with AnyWordSpecLike
    with Matchers
    with LogCapturing {

  "a pool router" should {
    "send messages in round-robing fashion" in {

      val probe = TestProbe[String]
      val worker = Worker(probe.ref)
      val router = spawn(Manager(worker), "round-robin")

      probe.expectMessage("hi")
      probe.receiveMessages(10)
    }

    "Broadcast, sending each message to all routtes" in {

      val probe = TestProbe[String]
      val worker = Worker(probe.ref)

      val router = spawn(BroadcastingManager(worker), "broadcasting")

      probe.expectMessage("hi, there")
      probe.receiveMessages(43)

    }

    "ConstantHashing send messages to only one" ignore {

      val probe = TestProbe[String]
      val worker = Worker(probe.ref)

      val router = spawn(Manager(worker), "constant-hashing")

      probe.expectMessage("hi")
      probe.receiveMessages(10)

    }
  }
  "a group router" should {
    "send messages to one worker registered at a key" ignore { // because the same key conflict with
      // next test "send messages to all worker registered at a key"
      val probe1 = TestProbe[String]
      val behavior1 =
        Behaviors.monitor(probe1.ref, Behaviors.empty[String])

      system.receptionist ! Receptionist.Register(
        PhotoProcessor.Key,
        spawn(behavior1))

      val groupRouter = spawn(Camera())
      groupRouter ! Camera.Photo("hi")

      probe1.expectMessage("hi")
      probe1.receiveMessages(1)

    }
    "send messages to all photo processors registered" in {
      val photoProcessor1 = TestProbe[String]
      val pp1Monitor = Behaviors.monitor(photoProcessor1.ref, PhotoProcessor())

      val photoProcessor2 = TestProbe[String]
      val pp2Monitor = Behaviors.monitor(photoProcessor2.ref, PhotoProcessor())

      system.receptionist ! Receptionist.Register(
        PhotoProcessor.Key,
        spawn(pp1Monitor))
      system.receptionist ! Receptionist.Register(
        PhotoProcessor.Key,
        spawn(pp2Monitor))

      val camera = spawn(Camera())
      camera ! Camera.Photo("A")
      camera ! Camera.Photo("B")

      photoProcessor1.receiveMessages(1)

      photoProcessor2.receiveMessages(1)
    }

    "will send messages with same id to the same aggregator" in {
      //Given there are two Aggregators
      val probe1 = TestProbe[Aggregator.Event]
      val probe2 = TestProbe[Aggregator.Event]

      spawn(Aggregator(forwardTo = probe1.ref), "aggregator1")
      spawn(Aggregator(forwardTo = probe2.ref), "aggregator2")

      val contentValidator = spawn(DataObfuscator(), "wa-1")
      val dataEnricher = spawn(DataEnricher(), "wb-1")

      //When a message with same id is sent to different actors
      contentValidator ! DataObfuscator.Message("123", "Text")
      dataEnricher ! DataEnricher.Message("123", "Text")
      //Then one aggregator receives both while the other receives none
      probe1.expectMessage(
        Aggregator.Completed("123", "text", "metadata"))
      probe2.expectNoMessage()

      //When a message with same id is sent to different actors
      contentValidator ! DataObfuscator.Message("z23", "LoreIpsum")
      dataEnricher ! DataEnricher.Message("z23", "LoreIpsum")
      //Then one aggregator receives both while the other receives none
      probe1.expectNoMessage()
      probe2.expectMessage(
        Aggregator.Completed("z23", "loreipsum", "metadata"))
    }
  }

  "A State Router" should {
    val forwardToProbe = TestProbe[String]
    val alertToProbe = TestProbe[String]
    val switch =
      spawn(Switch(forwardToProbe.ref, alertToProbe.ref), "switch")
    "route to forward to actor reference when on" in {
      switch ! Switch.Payload("content1", "metadata1")
      forwardToProbe.expectMessage("content1")
    }
    "route to alert actor and wait when off" in {
      switch ! Switch.SwitchOff
      switch ! Switch.Payload("content2", "metadata2")
      alertToProbe.expectMessage("metadata2")

    }

  }

}

object Switch {

  sealed trait Command
  case object SwitchOn extends Command
  case object SwitchOff extends Command
  case class Payload(content: String, metadata: String)
      extends Command

  def apply(
      forwardTo: ActorRef[String],
      alertTo: ActorRef[String]): Behavior[Command] =
    on(forwardTo, alertTo)

  def on(
      forwardTo: ActorRef[String],
      alertTo: ActorRef[String]): Behavior[Command] =
    Behaviors.receive { (context, message) =>
      message match {
        case SwitchOn =>
          context.log.warn("sent SwitchOn but was ON already")
          Behaviors.same
        case SwitchOff =>
          off(forwardTo, alertTo)
        case Payload(content, metadata) =>
          forwardTo ! content
          Behaviors.same
      }
    }

  def off(
      forwardTo: ActorRef[String],
      alertTo: ActorRef[String]): Behavior[Command] =
    Behaviors.receive { (context, message) =>
      message match {
        case SwitchOn =>
          on(forwardTo, alertTo)
        case SwitchOff =>
          context.log.warn("sent SwitchOff but was OFF already")
          Behaviors.same
        case Payload(content, metadata) =>
          alertTo ! metadata
          Behaviors.same
      }
    }
}

object DataObfuscator {
  sealed trait Command
  case class Message(id: String, content: String) extends Command

  def apply() = Behaviors.setup[Command] { context =>

    val router = context.spawnAnonymous {
      Routers
        .group(Aggregator.serviceKey)
        .withConsistentHashingRouting(10, Aggregator.mapping)
    }

    Behaviors.receiveMessage[Command] {
      case Message(id, content) =>
        //obfuscates sensitive data and
        router ! Aggregator
          .Obfuscated(id, content.toLowerCase)
        Behaviors.same
    }
  }
}

object DataEnricher {
  sealed trait Command
  case class Message(id: String, content: String) extends Command

  def apply() = Behaviors.setup[Command] { context =>

    val router = context.spawnAnonymous {
      Routers
        .group(Aggregator.serviceKey)
        .withConsistentHashingRouting(10, Aggregator.mapping)
    }

    Behaviors.receiveMessage[Command] {
      case Message(id, content) =>
        //fetches some metadata and
        router ! Aggregator.Enriched(id, "metadata")
        Behaviors.same
    }
  }
}

object Aggregator {

  val serviceKey = ServiceKey[Aggregator.Command]("agg-key")

  def mapping(command: Command) = command.id

  sealed trait Command {
    def id: String
  }
  case class Obfuscated(id: String, content: String) extends Command
  case class Enriched(id: String, metadata: String) extends Command

  sealed trait Event
  case class Completed(id: String, content: String, metadata: String)
      extends Event

  def apply(
      messages: Map[String, String] = Map(),
      forwardTo: ActorRef[Event]): Behavior[Command] =
    Behaviors.setup[Command] { context =>

      context.system.receptionist ! Receptionist.Register(
        serviceKey,
        context.self)

      Behaviors.receiveMessage {
        case v @ Obfuscated(id, content) => {
          messages.get(id) match {
            case Some(metadata) =>
              forwardTo ! Completed(id, content, metadata)
              apply(messages - (id, metadata), forwardTo)
            case None =>
              apply(messages + (id -> content), forwardTo)
          }
        }
        case e @ Enriched(id, metadata) => {
          messages.get(id) match {
            case Some(content) =>
              forwardTo ! Completed(id, content, metadata)
              apply(messages - (id, content), forwardTo)
            case None =>
              apply(messages + (id -> metadata), forwardTo)
          }
        }
      }
    }
}



object Worker {
  def apply(monitor: ActorRef[String]): Behavior[String] =
    Behaviors.receiveMessage[String] {
      case message =>
        monitor ! message
        Behaviors.same
    }
}

object Manager {
  def apply(behavior: Behavior[String]) = Behaviors.setup[Unit] {
    context =>
      val routingBehavior: Behavior[String] =
        Routers.pool(poolSize = 4)(behavior)
      val router: ActorRef[String] =
        context.spawn(routingBehavior, "test-pool")
      (0 to 10).foreach { n =>
        router ! "hi"
      }
      Behaviors.empty
  }
}

object BroadcastingManager {
  def apply(behavior: Behavior[String]) = Behaviors.setup[Unit] {
    context =>
      val poolSize = 4

      val routingBehavior: PoolRouter[String] =
        Routers
          .pool(poolSize = poolSize)(behavior)
          .withBroadcastPredicate(msg => msg.length > 5)

      val router: ActorRef[String] =
        context.spawn(routingBehavior, "test-pool")

      (0 to 10).foreach { n =>
        router ! "hi, there"
      }

      Behaviors.empty
  }
}

object PostalOffice {
  sealed trait Command
  case class Standard(msg: String) extends Command
  case class Tracked(msg: String) extends Command
  case class Guaranteed(msg: String) extends Command

  def apply(): Behavior[Command] =
    Behaviors.empty
}

object PostalOfficeManager {

  def apply(behavior: Behavior[PostalOffice.Command]) =
    Behaviors.setup[Unit] { context =>

      val routingBehavior: PoolRouter[PostalOffice.Command] =
        Routers
          .pool(poolSize = 4)(behavior)
          .withBroadcastPredicate(msg =>
            msg.isInstanceOf[PostalOffice.Guaranteed])

      val router: ActorRef[PostalOffice.Command] =
        context.spawn(routingBehavior, "test-pool")

      (0 to 10).foreach { n =>
        router ! PostalOffice.Guaranteed("payslip")
      }
      Behaviors.empty
    }
}

object PhotoProcessor {
  val Key = ServiceKey[String]("photo-procesor-key") 
  def apply(): Behavior[String] = Behaviors.ignore
}

object Camera {

  case class Photo(content: String) 

  def apply() = Behaviors.setup[Photo] { context =>

    val routingBehavior: GroupRouter[String] =
      Routers.group(PhotoProcessor.Key)
    val router: ActorRef[String] =
      context.spawn(routingBehavior, "photo-processor-pool")

    Behaviors.receiveMessage {
      case Photo(content) =>
          router ! content
          Behaviors.same
    }
  }
}
// import java.security.MessageDigest
// object ConstantHashingManager {
//   def apply(behavior: Behavior[String]) = Behaviors.setup[Unit] { context =>
//     val poolSize = 4

//     def md5(s: String) =
//       MessageDigest.getInstance("MD5").digest(s.getBytes)

//     val routingBehavior: PoolRouter[String] =
//       Routers.pool(poolSize = poolSize)(behavior)

//     routingBehavior.withConsistentHashingRouting(poolSize, md5)
//     val router: ActorRef[String] = context.spawn(routingBehavior, "test-pool")

//     (0 to 10).foreach { n =>
//       router ! "hi"
//     }
//     Behaviors.empty
//   }
// }
