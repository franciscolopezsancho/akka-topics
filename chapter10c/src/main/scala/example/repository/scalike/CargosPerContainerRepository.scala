package example.repository.scalike

import scalikejdbc._

trait CargosPerContainerRepository {

  def addCargo(containerId: String, session: ScalikeJdbcSession): Unit
}

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
}
