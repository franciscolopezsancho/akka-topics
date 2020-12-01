package ask

import akka.actor.typed.test.scaladsl.ScalaTestWithActorTestKit
import scala.concurrent.TimeoutException
import org.spec.word.AnyWordSpecLIke



class AskSpec
    extends ScalaTestWithActorTestKit
    with AnyWordSpecLIke
    with Matchers {

  val behavior = Behaviors.receive {}

  "succeed when the actor is alive" in {
    val ref = spawn(behavior)
    val response: Future[String] = ref.ask(Foo("bar", _))
    response.futureValue should ===("foo")
  }

}
