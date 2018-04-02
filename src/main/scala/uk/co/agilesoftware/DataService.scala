package uk.co.agilesoftware

import spray.json.DefaultJsonProtocol._
import spray.json._

import scala.concurrent.{ExecutionContext, Future}

trait DataService {
  protected def connector: Connector
  protected def fn: String => CollectedResponse
  protected val name: String

  def get(serviceParams: Map[String, String])(implicit ec: ExecutionContext): Future[CollectedResponse] =
    serviceParams.get(name) match {
      case Some(params) => connector.get(name, params)(fn)
      case None => Future.successful(Map())
    }
}

trait ShipmentDataService extends DataService {
  private val shipments = "shipments"
  override def fn: String => CollectedResponse = json => Map(shipments -> json.parseJson.convertTo[ShipmentResponse])
  override protected val name: String = shipments
}

object ShipmentDataService extends ShipmentDataService {
  override val connector: Connector = DownstreamConnector()
}

trait TrackDataService extends DataService {
  private val track = "track"
  override val fn: String => CollectedResponse = json => Map(track -> json.parseJson.convertTo[TrackResponse])
  override protected val name: String = track
}

object TrackDataService extends TrackDataService {
  override val connector: Connector = DownstreamConnector()
}

trait PricingDataService extends DataService {
  private val pricing = "pricing"
  override val fn: String => CollectedResponse = json => Map(pricing -> json.parseJson.convertTo[PricingResponse])
  override protected val name: String = pricing
}

object PricingDataService extends PricingDataService {
  override val connector: Connector = DownstreamConnector()
}


