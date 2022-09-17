package example.countwords

import akka.cluster.typed.{ Cluster, Subscribe }
import akka.cluster.typed.SelfUp
import akka.cluster.ClusterEvent.ClusterDomainEvent
import akka.actor.typed.scaladsl.{ ActorContext, Behaviors, Routers }
import akka.actor.typed.{ ActorSystem, Behavior }
import com.typesafe.config.ConfigFactory

object App {

  def main(args: Array[String]): Unit = {
    if (args.isEmpty) {
      startup("worker", 25251)
    } else {
      require(
        args.size == 2,
        "Usage: two params required 'role' and 'port'")
      startup(args(0), args(1).toInt)
    }
  }

  def startup(role: String, port: Int): Unit = {

    val config = ConfigFactory
      .parseString(s"""
      akka.remote.artery.canonical.port=$port
      akka.cluster.roles = [$role]
      """)
      .withFallback(ConfigFactory.load("words"))

    val guardian = ActorSystem(ClusteredGuardian(), "WordsCluster", config)
  }

  private object ClusteredGuardian {

    def apply(): Behavior[SelfUp] =
      Behaviors.setup[SelfUp] { context =>

        val cluster = Cluster(context.system)
        if (cluster.selfMember.hasRole("director")) {
          // instead of cluster.registerOnMemberUp now on typed
          Cluster(context.system).subscriptions ! Subscribe(
            context.self,
            classOf[SelfUp])
        }
        if (cluster.selfMember.hasRole("aggregator")) {
          val numberOfWorkers =
            context.system.settings.config
              .getInt("example.countwords.workers-per-node")
          for (i <- 0 to numberOfWorkers) {
            //with supervision resume
            context.spawn(Worker(), s"worker-$i")
          }
        }
        Behaviors.receiveMessage {
          case SelfUp(_) =>
            val router = context.spawnAnonymous {
              Routers
                .group(Worker.RegistrationKey)
            }
            context.spawn(Master(router), "master")
            Behaviors.same
        }
      }
  }
}
