package com.lightbend.akka.sample

import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.http.scaladsl.{Http, HttpExt}

import scala.concurrent.Future

trait Connector {
  def get(uri: Uri)(fn: String => CollectedResponse): Future[CollectedResponse]
}

class DownstreamConnector(http: HttpExt) extends Connector {
  import Singletons._

  def get(uri: Uri)(fn: String => CollectedResponse): Future[CollectedResponse] = {
    http.singleRequest(HttpRequest(uri = uri)).flatMap {
      case HttpResponse(StatusCodes.OK, _, entity, _) if entity.contentType == ContentTypes.`application/json` =>
         Unmarshal(entity).to[String].map(fn)
      case _ => Future.successful(Map.empty)
    }
  }
}

object DownstreamConnector {
  import Singletons._
  def apply(): Connector = new DownstreamConnector(Http())
}
