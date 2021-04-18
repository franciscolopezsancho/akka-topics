package example.persistence

import scalikejdbc.DBSession
import example.persistence.ShippingShardingVehicle
import example.projection.ScalikeJdbcSession

trait ParcelPerSizeRepository {
  def update(
      session: ScalikeJdbcSession,
      parcel: ShippingShardingVehicle.Parcel): Unit
  def getCount(
      session: ScalikeJdbcSession,
      parcelSize: String): Option[Int]
}

class InMemParcelPerSizeRepository extends ParcelPerSizeRepository {
  // parcels grouped by size
  var parcelsIdsPerSize: Map[String, Int] = Map.empty
  override def update(
      session: ScalikeJdbcSession,
      parcel: ShippingShardingVehicle.Parcel): Unit = {
    parcelsIdsPerSize =
      parcelsIdsPerSize + (parcel.size -> (parcelsIdsPerSize
        .getOrElse(parcel.size, 0) + 1))
  }

  override def getCount(
      session: ScalikeJdbcSession,
      parcelSize: String): Option[Int] =
    parcelsIdsPerSize.get(parcelSize)

}
