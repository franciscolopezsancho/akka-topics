package example.container.grpc

import example.container.grpc.{ AddedCargo, Cargo, ContainerService }
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class ContainerServiceImpl(
    implicit executionContext: ExecutionContext)
    extends ContainerService {

  override def addCargo(in: Cargo): Future[AddedCargo] = {
    Future.successful(AddedCargo(true))
  }

}
