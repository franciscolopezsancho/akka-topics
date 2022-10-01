package example.repository.scalike

import akka.actor.typed.ActorSystem
import com.typesafe.config.Config

import scalikejdbc.ConnectionPool
import scalikejdbc.DataSourceCloser
import scalikejdbc.DataSourceConnectionPool

import scalikejdbc.config.DBs
import scalikejdbc.config.TypesafeConfig
import scalikejdbc.config.TypesafeConfigReader
import scalikejdbc.config.NoEnvPrefix

import com.zaxxer.hikari.HikariDataSource

object ScalikeJdbcSetup {

  def init(system: ActorSystem[_]): Unit = {
    initFromConfig(system.settings.config)
  }

  private def initFromConfig(config: Config): Unit = {
    val dbs = new DBsFromConfig(config)
    dbs.loadGlobalSettings()

    val dataSource = buildDataSource(
      config.getConfig("jdbc-connection-settings"))

    ConnectionPool.singleton(
      new DataSourceConnectionPool(
        dataSource = dataSource,
        closer = HikariCloser(dataSource)))
  }

  private def buildDataSource(config: Config): HikariDataSource = {
    val dataSource = new HikariDataSource()
    dataSource.setPoolName("read-side-bet-connection-pool")
    dataSource.setMaximumPoolSize(
      config.getInt("connection-pool.max-pool-size"))

    val timeout =
      config.getDuration("connection-pool.timeout").toMillis
    dataSource.setConnectionTimeout(timeout)
    dataSource.setDriverClassName(config.getString("driver"))
    dataSource.setJdbcUrl(config.getString("url"))
    dataSource.setUsername(config.getString("user"))
    dataSource.setPassword(config.getString("password"))

    dataSource
  }

  private class DBsFromConfig(val config: Config)
      extends DBs
      with TypesafeConfigReader
      with TypesafeConfig
      with NoEnvPrefix

  private final case class HikariCloser(dataSource: HikariDataSource)
      extends DataSourceCloser {
    override def close(): Unit = dataSource.close()
  }

}
