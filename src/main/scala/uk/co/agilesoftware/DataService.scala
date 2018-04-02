package uk.co.agilesoftware

import scala.concurrent.Future

trait DataService {
  protected def connector: DownstreamConnector
  val name: String

  val params:List[String] = List.empty

  def get(serviceParams: List[String]): Future[CollectedResponse] = {
    connector.get(name, serviceParams)
  }
}

trait ShipmentDataService extends DataService {
  override val name: String = "shipments"
}

object ShipmentDataService extends ShipmentDataService {
  override val connector: DownstreamConnector = ShipmentsConnector
}

trait TrackDataService extends DataService {
  override val name: String = "track"
}

object TrackDataService extends TrackDataService {
  override val connector: DownstreamConnector = TrackConnector
}

trait PricingDataService extends DataService {
  override val name: String = "pricing"
}

object PricingDataService extends PricingDataService {
  override val connector: DownstreamConnector = PricingConnector
}


