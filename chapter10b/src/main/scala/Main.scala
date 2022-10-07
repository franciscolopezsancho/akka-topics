package example.persistence

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.stream.scaladsl.Source
import akka.persistence.query.PersistenceQuery
import akka.persistence.jdbc.query.scaladsl.JdbcReadJournal
import akka.NotUsed

object Main extends App {

  implicit val system = ActorSystem(Behaviors.ignore, "runner")

  val readJournal: JdbcReadJournal = PersistenceQuery(system)
    .readJournalFor[JdbcReadJournal](JdbcReadJournal.Identifier)

  val source: Source[String, NotUsed] = readJournal.persistenceIds

  source.runForeach(println)
}
