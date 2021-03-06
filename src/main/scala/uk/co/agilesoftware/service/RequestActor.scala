package uk.co.agilesoftware.service

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Timers}
import uk.co.agilesoftware.ConnectorResponse
import uk.co.agilesoftware.connector.DownstreamConnector
import uk.co.agilesoftware.service.QueueActor.{Append, FlushQueue}
import uk.co.agilesoftware.service.RequestActor._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object RequestActor {
  case class RequestFor(params: Seq[String])
  case class MakeACallWith(params: Seq[String])
  case object GetResponse
  case object ForceApiCall

  case object MaxWait
  def apply(queue: ActorRef, connector: DownstreamConnector, maxWait: FiniteDuration = 5.seconds)(implicit ec: ExecutionContext)
    = Props(new RequestActor(queue, connector, maxWait))
}

class RequestActor(queue: ActorRef, connector: DownstreamConnector, maxWait: FiniteDuration = 5.seconds)(implicit ec: ExecutionContext) extends Actor
  with ActorLogging with Timers {

  var mayBeResponse: Option[ConnectorResponse] = None
  var cachedParams = Seq.empty[String]

  override def receive: Receive = {
    case RequestFor(params) =>
      timers.startSingleTimer(MaxWait, ForceApiCall, maxWait)
      cachedParams = params
      queue ! Append(params)

    case MakeACallWith(myParamsPlusMore) =>
      timers.cancel(MaxWait)
      connector.get(myParamsPlusMore).map { res =>
        mayBeResponse = Some(res.filterKeys(cachedParams.contains(_)))
      }

    case ForceApiCall => queue ! FlushQueue
    case GetResponse => sender() ! mayBeResponse
  }
}



