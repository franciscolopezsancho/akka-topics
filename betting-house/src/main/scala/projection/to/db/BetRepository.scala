package example.repository.scalike

import scalikejdbc._

trait BetRepository {

  def addBet(
      betId: String,
      walletId: String,
      marketId: String,
      stake: Int,
      session: ScalikeJdbcSession): Unit
  def getBetPerMarketTotalStake(
      marketId: String,
      session: ScalikeJdbcSession): Option[Long]

}

class BetRepositoryImpl extends BetRepository {

  override def addBet(
      betId: String,
      walletId: String,
      marketId: String,
      stake: Int,
      session: ScalikeJdbcSession): Unit = {
    session.db.withinTx { implicit dbSession =>
      sql"""
				INSERT INTO bet_wallet_market (betId, walletId, marketId, stake) VALUES ($betId, $walletId, $marketId, $stake)
				ON CONFLICT (betId) DO NOTHING
			""".executeUpdate().apply()
    }

  }

  override def getBetPerMarketTotalStake(
      marketId: String,
      session: ScalikeJdbcSession): Option[Long] = {
    session.db.readOnly { implicit dbSession =>
      sql"""SELECT sum(stake) FROM bet_wallet_market WHERE marketId = $marketId GROUP BY marketId"""
        .map(rs => rs.long("sum"))
        .toOption()
        .apply()
    }
  }
}
