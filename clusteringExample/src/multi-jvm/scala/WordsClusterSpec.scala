package example.cluster

import akka.actor.testkit.typed.scaladsl.TestProbe
import com.typesafe.config.ConfigFactory
import akka.actor.typed.scaladsl.adapter._ //allows toTyped?
import akka.actor.typed.scaladsl.AskPattern._ //allows toTyped?
import akka.actor.typed.scaladsl.{Routers, Behaviors}
import akka.actor.typed.Behavior

import akka.cluster.Cluster
import akka.cluster.typed.SelfUp
import akka.cluster.ClusterEvent.{MemberUp, CurrentClusterState, ClusterDomainEvent}

import akka.remote.testkit.MultiNodeConfig

import scala.concurrent.duration._

object WordsClusterConfig extends MultiNodeConfig {
	
	val seed = role("seed")
	val director = role("director")
	val aggregator1 = role("aggregator1")
	val aggregator2 = role("aggregator2")

	nodeConfig(seed){ConfigFactory.parseString("""
	akka.actor.provider = cluster,
	akka.cluster.roles = [seed]
	""").withFallback(ConfigFactory.load())}
	nodeConfig(director){ConfigFactory.parseString("""
			akka.actor.provider = cluster,
	akka.cluster.roles = [director]
	""").withFallback(ConfigFactory.load())}
	nodeConfig(aggregator1){ConfigFactory.parseString("""
			akka.actor.provider = cluster,
	akka.cluster.roles = [aggregator]
	""").withFallback(ConfigFactory.load())}
	nodeConfig(aggregator2){ConfigFactory.parseString("""
			akka.actor.provider = cluster,
	akka.cluster.roles = [aggregator]
	""").withFallback(ConfigFactory.load())}



}


class WordsSampleSpecMultiJvmNode1 extends WordsClusterSpec
class WordsSampleSpecMultiJvmNode2 extends WordsClusterSpec
class WordsSampleSpecMultiJvmNode3 extends WordsClusterSpec
class WordsSampleSpecMultiJvmNode4 extends WordsClusterSpec

import akka.remote.testkit.MultiNodeSpec
import akka.testkit.ImplicitSender 
import org.scalatest.BeforeAndAfterAll
import org.scalatest.Matchers
import org.scalatest.WordSpecLike

abstract class WordsClusterSpec extends MultiNodeSpec(WordsClusterConfig)
	with WordSpecLike with Matchers with BeforeAndAfterAll
	with ImplicitSender {

		import WordsClusterConfig._


	override def initialParticipants = roles.size

	override def beforeAll() = multiNodeSpecBeforeAll()

	override def afterAll() = multiNodeSpecAfterAll()

	implicit val typedSystem = system.toTyped

	"The words example" should {
		"start up the cluster" in within(15.seconds) {
			Cluster(typedSystem).subscribe(testActor, classOf[MemberUp])
			expectMsgClass(classOf[CurrentClusterState])

			val seedAddress = node(seed).address
			val directorAddress = node(director).address
			val aggregator1Address = node(aggregator1).address
			val aggregator2Address  = node(aggregator2).address

			Cluster(typedSystem).join(seedAddress)

			receiveN(4).collect { case MemberUp(m) => m.address}.toSet should be(
				Set(seedAddress, directorAddress, aggregator1Address, aggregator2Address)
			)

			Cluster(typedSystem).unsubscribe(testActor)

			testConductor.enter("all-up")
		}

		"test we get results" ignore within(15.seconds) {
			runOn(aggregator1,aggregator2){
				// val guardian = system.spawn[SelfUp](App.ClusteredGuardian(), "guardian")	
			}
			runOn(director){
				val probe = TestProbe[Master.Event]
				 val router = system.spawn(
	            Routers
	              .group(Worker.RegistrationKey), "router")
	          
				val masterMonitored = Behaviors.monitor(probe.ref, Master(router))
	          val master = system.spawn(masterMonitored, "master")
	          master ! Master.Tick
	          awaitAssert {
	          	
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
			



			testConductor.enter("done-2")
		}



	}
}