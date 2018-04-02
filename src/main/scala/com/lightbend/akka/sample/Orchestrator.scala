package com.lightbend.akka.sample

import java.net.URL

import akka.actor.{ActorRef, ActorSystem}
import com.lightbend.akka.sample.ShipmentsActor.Get
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object Orchestrator {
  implicit val system: ActorSystem = ActorSystem("helloAkka")

  private val shipmentService: ActorRef = system.actorOf(ShipmentsActor.props, "shipmentsActor")

  implicit lazy val timeout: Timeout = Timeout(5.seconds)

  type CollectedResponse = Map[String, Map[String, _]]

  def execute(urls: Seq[URL]): Future[CollectedResponse] = {
    urls.find(url => url.getPath.equals("/shipments")) match {
      case Some(url) => (shipmentService ? Get(url)).map(_.asInstanceOf[CollectedResponse])
      case None => Future.successful(Map())
    }
  }
}
