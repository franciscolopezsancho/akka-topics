package example.cluster

import akka.cluster.Cluster
import akka.actor.typed.scaladsl.{ Behaviors, Routers }
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

    val guardian =
      ActorSystem[Nothing](
        ClusteredGuardian(),
        "WordsCluster",
        config)

  }

  private object ClusteredGuardian {

    def apply(): Behavior[Nothing] =
      Behaviors.setup[Nothing] { context =>
        val cluster = Cluster(context.system)
        if (cluster.selfMember.hasRole("master")) {
          val router = context.spawnAnonymous {
            Routers
              .group(Worker.RegistrationKey)
          }
          context.spawnAnonymous(Master(router))
        }
        if (cluster.selfMember.hasRole("worker")) {
          for (i <- 0 to 10) {
            //with supervision resume
            context.spawn(Worker(), s"worker-$i")
          }
        }
        Behaviors.empty
      }
  }
}
