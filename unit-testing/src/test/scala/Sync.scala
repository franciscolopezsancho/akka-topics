package some

import org.scalatest.{ BeforeAndAfterAll, Suite }
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import akka.actor.testkit.typed.scaladsl.BehaviorTestKit

class SyncTestingExampleSpec extends AnyWordSpec with Matchers {

  "Typed actor synchronous testing" must {

    "record spawning" in {
      //#test-child
      val testKit = BehaviorTestKit(Hello())
      testKit.run(Hello.CreateChild("child"))
      testKit.expectEffect(Spawned(childActor, "child"))
      //#test-child
    }
  }
}

object Hello {

  sealed trait Command
  case object CreateAnonymousChild extends Command

}

trait StopSystemAfterAll extends BeforeAndAfterAll {
  this: TestKit with Suite =>
  override protected def afterAll() {
    super.afterAll()
    system.shutdown()
  }
}
