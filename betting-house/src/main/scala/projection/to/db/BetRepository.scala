package example.repository.scalike

import scalikejdbc._

case class StakePerResult(sum: Double, result: Int)

trait BetRepository {

  def addBet(
      betId: String,
      walletId: String,
      marketId: String,
      odds: Double,
      stake: Int,
      result: Int,
      session: ScalikeJdbcSession): Unit
  def getBetPerMarketTotalStake(
      marketId: String,
      session: ScalikeJdbcSession): List[StakePerResult]

}

class BetRepositoryImpl extends BetRepository {

  override def addBet(
      betId: String,
      walletId: String,
      marketId: String,
      odds: Double,
      stake: Int,
      result: Int,
      session: ScalikeJdbcSession): Unit = {
    session.db.withinTx { implicit dbSession =>
      sql"""
				INSERT INTO bet_wallet_market (betId, walletId, marketId, odds, stake, result) VALUES ($betId, $walletId, $marketId, $odds, $stake, $result)
				ON CONFLICT (betId) DO NOTHING
			""".executeUpdate().apply()
    }

  }

  override def getBetPerMarketTotalStake(
      marketId: String,
      session: ScalikeJdbcSession): List[StakePerResult] = {
    session.db.readOnly { implicit dbSession =>
      sql"""SELECT sum(stake * odds), result FROM bet_wallet_market WHERE marketId = $marketId GROUP BY marketId, result"""
        .map(rs => StakePerResult(rs.double("sum"), rs.int("result")))
        .list
        .apply()
    }
  }
}
