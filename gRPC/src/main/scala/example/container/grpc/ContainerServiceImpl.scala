package example.container.grpc

import akka.stream.Materializer
import example.container.grpc.{ AddedCargo, Cargo, ContainerService }

import scala.concurrent.Future

class ContainerServiceImpl(implicit mat: Materializer)
    extends ContainerService {
  import mat.executionContext

  override def addCargo(in: Cargo): Future[AddedCargo] = {
    Future.successful(AddedCargo(true))
  }

}
