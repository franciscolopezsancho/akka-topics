package example.persistence

import org.slf4j.LoggerFactory

import akka.actor.typed.{ ActorRef, ActorSystem, Behavior }
import akka.actor.typed.scaladsl.Behaviors

import akka.stream.scaladsl.Source

import akka.persistence.query.{
  EventEnvelope,
  Offset,
  PersistenceQuery
}
import akka.persistence.jdbc.query.scaladsl.JdbcReadJournal

import akka.NotUsed

object Main {

  //write
  val logger = LoggerFactory.getLogger(Main + "")

  def main(args: Array[String]): Unit = {
    val system =
      ActorSystem[Nothing](Behaviors.empty, "persistence-query")
    val readJournal = PersistenceQuery(system)
      .readJournalFor[JdbcReadJournal](JdbcReadJournal.Identifier)
    val source: Source[EventEnvelope, NotUsed] =
      readJournal.eventsByTag("tag-0", Offset.noOffset)
  }

}
