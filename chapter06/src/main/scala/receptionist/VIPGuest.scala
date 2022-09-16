package receptionist

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.receptionist.Receptionist

object VIPGuest {

  sealed trait Command
  case object EnterHotel extends Command
  case object LeaveHotel extends Command

  def apply() = Behaviors.receive[Command] { (context, message) =>
    message match {
      case EnterHotel =>
        context.system.receptionist ! Receptionist
          .Register(HotelConcierge.GoldenKey, context.self)
        Behaviors.same

      case LeaveHotel =>
        context.system.receptionist ! Receptionist
          .Deregister(HotelConcierge.GoldenKey, context.self)
        Behaviors.same
    }

  }
}