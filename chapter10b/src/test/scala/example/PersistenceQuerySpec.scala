import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit

import akka.stream.scaladsl.{ Keep, Sink, Source }

import akka.persistence.query.{
  EventEnvelope,
  Offset,
  PersistenceQuery
}

import akka.persistence.jdbc.query.scaladsl.JdbcReadJournal

import akka.NotUsed

import akka.stream.testkit.scaladsl.{ TestSink }
import akka.stream.testkit.TestSubscriber

import example.persistence.SPContainer

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
        "spcontainer-type-key|9",
        "spcontainer-type-key|11")

    }
  }
  //Bear in mind this test depends on the order you
  // entered the items when following chapter09b/README.md
  // plus comparing to EventEnvelope is done with the following:
  //  override def equals(obj: Any): Boolean = obj match {
  //   case other: EventEnvelope =>
  //     offset == other.offset && persistenceId == other.persistenceId && sequenceNr == other.sequenceNr &&
  //     event == other.event // timestamp && metadata not included in equals for backwards compatibility
  //   case _ => false
  //  }

  "a persistence query" should {
    "retrieve the data from db by tag" in {

      val readJournal = PersistenceQuery(system)
        .readJournalFor[JdbcReadJournal](JdbcReadJournal.Identifier)

      val source: Source[EventEnvelope, NotUsed] =
        readJournal.eventsByTag("container-tag-0", Offset.noOffset)

      val consumer
          : Sink[EventEnvelope, TestSubscriber.Probe[EventEnvelope]] =
        TestSink[EventEnvelope]()

      val probe: TestSubscriber.Probe[EventEnvelope] =
        source.toMat(consumer)(Keep.right).run

      val s = probe.expectSubscription
      s.request(2)
      probe.expectNextUnordered(
        new EventEnvelope(
          akka.persistence.query.Sequence(1),
          "spcontainer-type-key|9",
          1L,
          SPContainer
            .CargoAdded("9", SPContainer.Cargo("456", "sack", 22)),
          0L),
        new EventEnvelope(
          akka.persistence.query.Sequence(2),
          "spcontainer-type-key|9",
          2L,
          SPContainer
            .CargoAdded("9", SPContainer.Cargo("459", "bigbag", 15)),
          0L))

    }
  }

}
//
