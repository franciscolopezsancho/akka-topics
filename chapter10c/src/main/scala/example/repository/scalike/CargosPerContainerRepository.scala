package example.repository.scalike

import scalikejdbc._
//TODO abstract to at least JdbcConection
trait CargosPerContainerRepository {

  def addCargo(containerId: String, session: ScalikeJdbcSession): Unit
  def getTotals(session: ScalikeJdbcSession)(
      implicit dbSession: DBSession): Map[String, Int]

}
//TODO create a query that group per kind of cargo??
class CargosPerContainerRepositoryImpl
    extends CargosPerContainerRepository {

  override def addCargo(
      containerId: String,
      session: ScalikeJdbcSession): Unit = {
    session.db.withinTx { implicit dbSession =>
      sql"""
				INSERT INTO cargos_per_container (containerId, cargos) VALUES ($containerId, 1)
				ON CONFLICT (containerId) DO 
          UPDATE SET cargos = cargos_per_container.cargos + 1
			""".executeUpdate().apply()
    }

  }

  override def getTotals(session: ScalikeJdbcSession)(
      implicit dbSession: DBSession): Map[String, Int] = {
    sql"SELECT containerId, cargos from cargos_per_container"
      .map(rs => (rs.string("containerId"), rs.int("cargos")))
      .list
      .apply()
      .toMap
  }
}
