package uk.co.agilesoftware

import akka.actor.{Actor, ActorRef, Props}
import uk.co.agilesoftware.QueueActor.Append
import uk.co.agilesoftware.RequestActor.MakeACallWith

import scala.collection.mutable

object QueueActor {
  case class Append(params: Seq[String])

  def props(maxQueueSize: Int = 5): Props = Props(new QueueActor(maxQueueSize))
}

class QueueActor(maxQueueSize: Int) extends Actor {
  val subscribers: mutable.Queue[ActorRef] = new mutable.Queue()
  val paramQueue: mutable.Queue[String] = new mutable.Queue()

  /**
    * Add params to the queue and
    * If queue is full broadcast all the params back to the senders Else do nothing
    * @return
    */
  override def receive: Receive = {
    case Append(params) =>
      subscribers.enqueue(sender())
      paramQueue.enqueue(params:_*)
      if(paramQueue.size >= maxQueueSize) {
        val allParams = paramQueue.dequeueAll(_ => true)
        subscribers.dequeueAll(_ => true).foreach { subscriber =>
          subscriber ! MakeACallWith(allParams)
        }
      }
  }
}