package example.repository.scalike

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
