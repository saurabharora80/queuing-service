package uk.co.agilesoftware

import scala.concurrent.{ExecutionContext, Future}

trait DataService {
  protected def connector: DownstreamConnector
  protected val name: String

  def get(serviceParams: Map[String, String])(implicit ec: ExecutionContext): Future[CollectedResponse] =
    serviceParams.get(name) match {
      case Some(params) => connector.get(name, params)
      case None => Future.successful(Map())
    }
}

trait ShipmentDataService extends DataService {
  override protected val name: String = "shipments"
}

object ShipmentDataService extends ShipmentDataService {
  override val connector: DownstreamConnector = ShipmentsConnector
}

trait TrackDataService extends DataService {
  override protected val name: String = "track"
}

object TrackDataService extends TrackDataService {
  override val connector: DownstreamConnector = TrackConnector
}

trait PricingDataService extends DataService {
  override protected val name: String = "pricing"
}

object PricingDataService extends PricingDataService {
  override val connector: DownstreamConnector = PricingConnector
}


