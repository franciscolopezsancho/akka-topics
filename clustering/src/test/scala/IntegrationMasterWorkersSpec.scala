package example.cluster

import akka.actor.testkit.typed.scaladsl.{
  LogCapturing,
  ScalaTestWithActorTestKit
}
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{ Behaviors, Routers }

import com.typesafe.config.{ ConfigFactory }

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class WordsSampleSpec
    extends ScalaTestWithActorTestKit(
      ConfigFactory
        .parseString("""example.cluster.workers-per-node = 5""")
        .withFallback(ConfigFactory.load("words")))
    with AnyWordSpecLike
    with Matchers
    with LogCapturing {

  "The words app" should {
    "send work from the master to the workers and back" in {
      //>> emulating guardian
      val numberOfWorkers =
        system.settings.config
          .getInt("example.cluster.workers-per-node")

      for (i <- 0 to numberOfWorkers) {
        spawn(Worker(), s"worker-$i")
      }

      val router = spawn {
        Routers
          .group(Worker.RegistrationKey)
      }
      val probe = createTestProbe[Master.Event]
      val masterMonitored =
        Behaviors.monitor(probe.ref, Master(router))
      spawn(masterMonitored, "master0")
      //<<

      probe.expectMessage(Master.Tick)
      probe.expectMessage(
        Master.CountedWords(
          Map(
            "this" -> 1,
            "a" -> 2,
            "very" -> 1,
            "simulates" -> 1,
            "simple" -> 1,
            "stream" -> 2)))
    }
  }
}
