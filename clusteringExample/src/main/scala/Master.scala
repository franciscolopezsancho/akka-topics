package example.cluster

import akka.actor.typed.{ ActorRef, Behavior }
import akka.actor.typed.scaladsl.{ Behaviors, GroupRouter, Routers }
import akka.actor.typed.receptionist.{ Receptionist, ServiceKey }

import scala.util.{ Failure, Success }
import akka.util.Timeout
import scala.concurrent.duration._

object Master {

  sealed trait Event
  case object Tick extends Event

  case class CountedWords(aggregation: Map[String, Int])
      extends Event
      with CborSerializable
  case class FailedJob(text: String) extends Event

  def apply(workerRouter: ActorRef[Worker.Command]): Behavior[Event] =
    Behaviors.withTimers { timers =>
      timers.startTimerWithFixedDelay(Tick, Tick, 1.second)
      working(workerRouter)
    }

  def working(
      workersRouter: ActorRef[Worker.Command],
      countedWords: Map[String, Int] = Map(),
      lag: Vector[String] = Vector()): Behavior[Event] =
    Behaviors.setup[Event] { context =>

      implicit val timeout: Timeout = 3.seconds
      Behaviors.receiveMessage[Event] {
        case Tick =>
          context.log.debug(s"receiving, current lag ${lag.size} ")

          val text = "this simulates a stream, a very simple stream"
          val allTexts = lag :+ text
          val (firstPart, secondPart) = allTexts.splitAt(5)
          firstPart.map { text =>
            context.ask(workersRouter, Worker.Process(text, _)) {
              case Success(CountedWords(map)) =>
                CountedWords(map) //weird
              case Failure(ex) =>
                context.log.warn(
                  s"error processing '$text'. Exception found: ${ex.toString()}")
                FailedJob(text)
            }
          }
          working(workersRouter, countedWords, secondPart)
        case CountedWords(map) =>
          val merged = merge(countedWords, map)
          context.log.debug(s"current count ${merged.toString} ")
          working(workersRouter, merged, lag)
        case FailedJob(text) =>
          context.log.debug(
            s"failed, adding text to lag ${lag.size} ")
          working(workersRouter, countedWords, lag :+ text)
      }
    }

  def merge(
      currentCount: Map[String, Int],
      newCount2Add: Map[String, Int]): Map[String, Int] =
    (currentCount.toSeq ++ newCount2Add)
      .groupMapReduce(_._1)(_._2)(_ + _)

}
