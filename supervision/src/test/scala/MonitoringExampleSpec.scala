import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.SupervisorStrategy

import akka.actor.testkit.typed.scaladsl.{
  LogCapturing,
  LoggingTestKit,
  ScalaTestWithActorTestKit,
  TestProbe
}

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class MonitoringExampleSpec
    extends ScalaTestWithActorTestKit
    with AnyWordSpecLike
    with Matchers
    with LogCapturing {

  "Among two actors, NO parent/child related, the watcher" must {
    "be able to be notified with Terminated when watched actor stops" in {
      val watcher = spawn(Watcher())
      val behavior = spawn(Behaviors.receiveMessagePartial[String] {
        case "stop" =>
          Behaviors.stopped
      })

      watcher.ref ! Watcher.Watch(behavior.ref)

      LoggingTestKit.info("terminated").expect {
        behavior ! "stop"
      }
    }
    "be able to watch a non child failing actor but only getting Terminated" in {
      val watcher = spawn(Watcher())
      val behavior = spawn(Behaviors.receiveMessagePartial[String] {
        case "exception" =>
          throw new IllegalStateException()
      })

      watcher.ref ! Watcher.Watch(behavior.ref)

      LoggingTestKit.info("terminated").expect {
        behavior ! "exception"
      }
    }

  }

  "Among two actors, parent/child related, the watcher " must {

    "getting notified by ChildFailed if it is a child that failed" in {
      val probe = TestProbe[String]
      val watcher = spawn(ParentWatcher(probe.ref))
      watcher.ref ! ParentWatcher.Spawn(ParentWatcher.childBehavior)

      watcher.ref ! ParentWatcher.FailChildren
      probe.expectMessage("childFailed")
    }
    "getting notified by Termination if it is a child that only stopped" in {
      val probe = TestProbe[String]
      val watcher = spawn(ParentWatcher(probe.ref))

      watcher.ref ! ParentWatcher.Spawn(ParentWatcher.childBehavior)
      watcher.ref ! ParentWatcher.StopChildren
      probe.expectMessage("terminated")
    }

    val restartingChildBehavior = Behaviors
      .supervise(ParentWatcher.childBehavior)
      .onFailure(SupervisorStrategy.restart)

    "is not being notified if the watched child throws an Non-Fatal Exception while having a restart strategy" in {
      val probe = TestProbe[String]
      val watcher = spawn(ParentWatcher(probe.ref))

      watcher.ref ! ParentWatcher.Spawn(restartingChildBehavior)
      watcher.ref ! ParentWatcher.FailChildren
      probe.expectNoMessage()
    }

    "being notified if child with restart strategy gets stopped" in {
      val probe = TestProbe[String]
      val watcher = spawn(ParentWatcher(probe.ref))

      watcher.ref ! ParentWatcher.Spawn(restartingChildBehavior)
      watcher.ref ! ParentWatcher.StopChildren
      probe.expectMessage("terminated")
    }
  }
}
