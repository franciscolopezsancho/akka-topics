import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit

import akka.stream.scaladsl.{ Keep, Sink, Source }

import akka.persistence.query.{
  EventEnvelope,
  Offset,
  PersistenceQuery
}
import akka.persistence.query.scaladsl.{ ReadJournal }

import akka.persistence.jdbc.query.scaladsl.JdbcReadJournal

import akka.NotUsed

import akka.stream.testkit.scaladsl.{ TestSink }
import akka.stream.testkit.TestSubscriber

class PersistenceQuerySpec
    extends ScalaTestWithActorTestKit
    with AnyWordSpecLike
    with Matchers {

  "a persistence query" should {
    "retrieve the persistenceIds from db and printing them" in {

      val readJournal: JdbcReadJournal = PersistenceQuery(system)
        .readJournalFor[JdbcReadJournal](JdbcReadJournal.Identifier)

      val source: Source[String, NotUsed] = readJournal.persistenceIds

      source.runForeach(println)
    }
  }

  "a persistence query" should {
    "retrieve the events from db and printing them" in {

      val readJournal: JdbcReadJournal = PersistenceQuery(system)
        .readJournalFor[JdbcReadJournal](JdbcReadJournal.Identifier)

      val source: Source[EventEnvelope, NotUsed] =
        readJournal.eventsByTag("container-tag-0", Offset.noOffset)

      source.runForeach(each => println(each.event))
      Thread.sleep(1000)
    }
  }

  "a persistence query" should {
    "retrieve the data from db" in {

      val readJournal = PersistenceQuery(system)
        .readJournalFor[JdbcReadJournal](JdbcReadJournal.Identifier)

      val source: Source[String, NotUsed] =
        readJournal.persistenceIds

      val consumer: Sink[String, TestSubscriber.Probe[String]] =
        TestSink[String]()

      val probe: TestSubscriber.Probe[String] =
        source.toMat(consumer)(Keep.right).run

      val s = probe.expectSubscription
      s.request(3)
      probe.expectNextUnordered(
        "scontainer-type-key|9",
        "scontainer-type-key|11")

    }
  }

}
//
