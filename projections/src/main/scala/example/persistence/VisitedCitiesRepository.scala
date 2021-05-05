package example.persistence

import scalikejdbc._

trait VisitedCitiesRepository {
  def addCity(session: ScalikeJdbcSession, city: String): Unit
  def getVisitedCities(session: ScalikeJdbcSession)(
      implicit dbSession: DBSession): Map[String, Int]
}

// class InMemParcelPerSizeRepository extends VisitedCitiesRepository {
//   // parcels grouped by size
//   var visitedCities: Map[String, Int] = Map.empty
//   override def addCity(
//       session: ScalikeJdbcSession,
//       city: String): Unit = {
//     visitedCities =
//       visitedCities + (city -> (visitedCities
//         .getOrElse(city, 0) + 1))
//   }

//   override def getVisitedCities(
//       session: ScalikeJdbcSession): Map[String, Int] = visitedCities

// }

class VisitedCitiesRepositoryDBImpl()
    extends VisitedCitiesRepository {

  override def addCity(
      session: ScalikeJdbcSession,
      city: String): Unit = {
    session.db.withinTx { implicit dbSession =>
      sql"""
          INSERT INTO visited_cities (city, count) VALUES ($city, 1)
          ON CONFLICT (city) DO UPDATE SET count = visited_cities.count + 1
      """.executeUpdate().apply()
    }
  }

  override def getVisitedCities(session: ScalikeJdbcSession)(
      implicit dbSession: DBSession): Map[String, Int] = {
    sql"SELECT city, count FROM visited_cities"
      .map(rs => (rs.string("city"), rs.int("count")))
      .list
      .apply()
      .toMap

  }
}

import java.sql.Connection
import scalikejdbc.DB
import akka.projection.jdbc.JdbcSession
import akka.japi.function.Function

/**
 * Provide database connections within a transaction to Akka Projections.
 */
final class ScalikeJdbcSession extends JdbcSession {
  val db: DB = DB.connect()
  db.autoClose(false)

  override def withConnection[Result](
      func: Function[Connection, Result]): Result = {
    db.begin()
    db.withinTxWithConnection(func(_))
  }

  override def commit(): Unit = db.commit()

  override def rollback(): Unit = db.rollback()

  override def close(): Unit = db.close()
}
