package uk.co.agilesoftware

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import uk.co.agilesoftware.QueueActor._
import uk.co.agilesoftware.RequestActor._

import scala.concurrent.ExecutionContext

object RequestActor {
  case class RequestFor(params: Seq[String])
  case class MakeACallWith(params: Seq[String])
  case object GetResponse

  def apply(queueActor: ActorRef, connector: ApiConnector)(implicit ec: ExecutionContext) = Props(new RequestActor(queueActor, connector))
}

class RequestActor(queue: ActorRef, connector: ApiConnector)(implicit ec: ExecutionContext) extends Actor with ActorLogging {

  var mayBeResponse: Option[ApiResponse] = None
  var cachedParams = Seq.empty[String]

  override def receive: Receive = {
    case RequestFor(params) =>
      cachedParams = params
      queue ! Append(params)

    case MakeACallWith(myParamsPlusMore) =>
      connector.get(myParamsPlusMore).map { res =>
        mayBeResponse = Some(res.filterKeys(cachedParams.contains(_)))
      }

    case GetResponse => sender() ! mayBeResponse
  }
}



