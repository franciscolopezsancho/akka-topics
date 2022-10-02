package subscriptions

import akka.actor.typed.PostStop
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.ClusterEvent.ClusterDomainEvent
import akka.cluster.ClusterEvent.MemberExited
import akka.cluster.ClusterEvent.MemberRemoved
import akka.cluster.ClusterEvent.MemberUp
import akka.cluster.ClusterEvent.ReachableMember
import akka.cluster.ClusterEvent.UnreachableMember
import akka.cluster.MemberStatus
import akka.cluster.typed.Cluster
import akka.cluster.typed.Subscribe
import akka.cluster.typed.Unsubscribe

object ClusterDomainEventListener {

  def apply() = Behaviors.setup[ClusterDomainEvent] { context =>
    Cluster(context.system).subscriptions ! Subscribe(
      context.self,
      classOf[ClusterDomainEvent])

    Behaviors
      .receiveMessage[ClusterDomainEvent] {
        case MemberUp(member) =>
          context.log.info(s"$member UP.")
          Behaviors.same
        case MemberExited(member) =>
          context.log.info(s"$member EXITED.")
          Behaviors.same
        case MemberRemoved(m, previousState) =>
          if (previousState == MemberStatus.Exiting) {
            context.log.info(s"Member $m gracefully exited, REMOVED.")
          } else {
            context.log.info(s"$m downed after unreachable, REMOVED.")
          }
          Behaviors.same
        case UnreachableMember(m) =>
          context.log.info(s"$m UNREACHABLE")
          Behaviors.same
        case ReachableMember(m) =>
          context.log.info(s"$m REACHABLE")
          Behaviors.same
        case event =>
          context.log.info(s"not handling ${event.toString}")
          Behaviors.same

      }
      .receiveSignal {
        case (context, PostStop) =>
          Cluster(context.system).subscriptions ! Unsubscribe(
            context.self)
          Behaviors.stopped
      }
  }
}
