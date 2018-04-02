package uk.co.agilesoftware

import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.http.scaladsl.{Http, HttpExt}

import scala.concurrent.Future

trait Connector {
  def get(name: String, params: String)(fn: String => CollectedResponse): Future[CollectedResponse]
}

class DownstreamConnector(serviceBaseUrl: String) extends Connector {
  import Singletons._

  private val http = Http()

  def get(name: String, params: String)(fn: String => CollectedResponse): Future[CollectedResponse] = {
    http.singleRequest(HttpRequest(uri = s"$serviceBaseUrl/$name?q=$params")).flatMap {
      case HttpResponse(StatusCodes.OK, _, entity, _) if entity.contentType == ContentTypes.`application/json` =>
         Unmarshal(entity).to[String].map(fn)
      case _ => Future.successful(Map.empty)
    }
  }
}

object DownstreamConnector {
  //host should be read from configuration
  def apply(host: String = "http://domain.com"): Connector = new DownstreamConnector(host)
}
