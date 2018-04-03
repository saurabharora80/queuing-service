package uk.co.agilesoftware.service

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import uk.co.agilesoftware.Singletons.system
import uk.co.agilesoftware._
import uk.co.agilesoftware.connector.{DownstreamConnector, PricingConnector, ShipmentsConnector, TrackConnector}
import uk.co.agilesoftware.service.RequestActor.{GetResponse, RequestFor}

import scala.concurrent.duration._
import scala.concurrent.{Future, Promise}

trait DataService {
  import Singletons._

  protected def connector: DownstreamConnector
  protected def queue: ActorRef
  val name: String
  protected val forcePullResponseIn: FiniteDuration = 5.seconds

  private implicit lazy val actorTimeout: Timeout = Timeout(5.seconds)

  def get(serviceParams: Seq[String]): Future[Data] = {
    def scheduleDelayedPoll(promise: Promise[Data], request: ActorRef): Unit = {
      system.scheduler.schedule(100 milliseconds, 100 milliseconds) {
        pollForData(promise, request)
      }
    }

    def pollForData(promise: Promise[Data], request: ActorRef): Unit = {
      (request ? GetResponse).map {
        case Some(response) => promise.success(Map(name -> response.asInstanceOf[ConnectorResponse]))
        case None => scheduleDelayedPoll(promise, request)
      }.recover {
        case ex => promise.failure(ex)
      }
    }

    val request = system.actorOf(RequestActor(queue, connector, forcePullResponseIn))

    request ! RequestFor(serviceParams)

    val pResponse = Promise[Data]
    pollForData(pResponse, request)
    pResponse.future
  }

}

trait ShipmentDataService extends DataService {
  override val name: String = "shipments"
}

object ShipmentDataService extends ShipmentDataService {
  override val connector: DownstreamConnector = ShipmentsConnector
  override protected val queue: ActorRef = system.actorOf(QueueActor())
}

trait TrackDataService extends DataService {
  override val name: String = "track"
}

object TrackDataService extends TrackDataService {
  override val connector: DownstreamConnector = TrackConnector
  override protected val queue: ActorRef = system.actorOf(QueueActor())
}

trait PricingDataService extends DataService {
  override val name: String = "pricing"
}

object PricingDataService extends PricingDataService {
  override val connector: DownstreamConnector = PricingConnector
  override protected val queue: ActorRef = system.actorOf(QueueActor())
}


