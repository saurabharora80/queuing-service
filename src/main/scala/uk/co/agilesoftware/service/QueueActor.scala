package uk.co.agilesoftware.service

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import uk.co.agilesoftware.service.QueueActor.{Append, FlushQueue}
import uk.co.agilesoftware.service.RequestActor.MakeACallWith

import scala.collection.mutable

object QueueActor {
  case class Append(params: Seq[String])
  case object FlushQueue

  //This should be read from config
  def apply(maxQueueSize: Int = 5): Props = Props(new QueueActor(maxQueueSize))
}

class QueueActor(maxQueueSize: Int) extends Actor with ActorLogging {
  val subscribers: mutable.Queue[ActorRef] = new mutable.Queue()
  val paramQueue: mutable.Queue[String] = new mutable.Queue()


  override def receive: Receive = {
    /**
      * Add params to the queue and
      * If queue is full broadcast all the params back to the senders Else do nothing
      * @return
      */
    case Append(params) =>
      subscribers.enqueue(sender())
      paramQueue.enqueue(params:_*)
      if(paramQueue.size >= maxQueueSize) informSubscribers()
    case FlushQueue => informSubscribers()
  }

  private def informSubscribers(): Unit = {
    val allParams = paramQueue.dequeueAll(_ => true)
    subscribers.dequeueAll(_ => true).foreach { subscriber =>
      subscriber ! MakeACallWith(allParams)
    }
  }
}