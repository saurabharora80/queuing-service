package com.lightbend.akka.sample

import akka.http.scaladsl.model.Uri
import spray.json.DefaultJsonProtocol._
import spray.json._

import scala.concurrent.{ExecutionContext, Future}

trait DataService {
  private val connector: Connector = DownstreamConnector()

  protected def fn: String => CollectedResponse

  protected val path: String

  def get(mappedUris: Map[String, Uri])(implicit ec: ExecutionContext): Future[CollectedResponse] =
    mappedUris.get(path) match {
      case Some(url) => connector.get(url)(fn)
      case None => Future.successful(Map())
    }
}

object ShipmentDataService extends DataService {
  private val shipments = "shipments"
  override def fn: String => CollectedResponse = json => Map(shipments -> json.parseJson.convertTo[ShipmentResponse])
  override protected val path: String = s"/$shipments"
}

object TrackDataService extends DataService {
  private val track = "track"
  override val fn: String => CollectedResponse = json => Map(track -> json.parseJson.convertTo[TrackResponse])
  override protected val path: String = s"/$track"
}

object PricingDataService extends DataService {
  private val pricing = "pricing"
  override val fn: String => CollectedResponse = json => Map(pricing -> json.parseJson.convertTo[PricingResponse])
  override protected val path: String = s"/$pricing"
}
