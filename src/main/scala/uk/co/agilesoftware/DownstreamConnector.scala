package uk.co.agilesoftware

import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import spray.json.DefaultJsonProtocol._
import spray.json._

import scala.concurrent.Future

trait DownstreamConnector {
  import Singletons._

  private val http = Http()

  val serviceBaseUrl: String
  val name: String
  def fn: String => ConnectorResponse

  def get(params: Seq[String]): Future[ConnectorResponse] = {
    http.singleRequest(HttpRequest(uri = s"$serviceBaseUrl/$name?q=${params.mkString(",")}")).flatMap {
      case HttpResponse(StatusCodes.OK, _, entity, _) if entity.contentType == ContentTypes.`application/json` =>
         Unmarshal(entity).to[String].map(fn)
      case _ => Future.successful(Map.empty)
    }
  }
}

trait ShipmentsConnector extends DownstreamConnector {
  override def fn: String => ConnectorResponse = json => json.parseJson.convertTo[ShipmentResponse]
}

object ShipmentsConnector extends ShipmentsConnector {
  //should be read from configuration
  override val serviceBaseUrl: String = "http://domain.com"
  override val name: String = "shipments"
}

trait TrackConnector extends DownstreamConnector {
  override def fn: String => ConnectorResponse = json => json.parseJson.convertTo[TrackResponse]
}

object TrackConnector extends TrackConnector {
  //should be read from configuration
  override val serviceBaseUrl: String = "http://domain.com"
  override val name: String = "track"
}

trait PricingConnector extends DownstreamConnector {
  override def fn: String => ConnectorResponse = json => json.parseJson.convertTo[PricingResponse]
}

object PricingConnector extends PricingConnector {
  //should be read from configuration
  override val serviceBaseUrl: String = "http://domain.com"
  override val name: String = "pricing"
}