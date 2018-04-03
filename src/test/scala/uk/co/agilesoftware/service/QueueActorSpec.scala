package uk.co.agilesoftware.service

import akka.testkit.TestProbe
import uk.co.agilesoftware.service.QueueActor.{Append, FlushQueue}
import uk.co.agilesoftware.service.RequestActor.MakeACallWith

class QueueActorSpec extends ActorSpec("QueueActorSpec") {

  "actor" should {
    "add params to the queue" in {
      val queue = system.actorOf(QueueActor())

      queue ! Append(Seq("one"))

      expectNoMessage(maxWaitForMsg)
    }

    "return all params if queue is full" in {
      val queue = system.actorOf(QueueActor(3))

      queue ! Append(Seq("one"))

      expectNoMessage(maxWaitForMsg)

      queue ! Append(Seq("two", "three"))

      expectMsg(MakeACallWith(Seq("one", "two", "three")))
    }

    "return all params if queue is full in the first request" in {
      val queue = system.actorOf(QueueActor(3))

      queue ! Append(Seq("one", "two", "three"))

      expectMsg(MakeACallWith(Seq("one", "two", "three")))
    }

    "return all params to all subscribers if queue is full" in {
      val queue = system.actorOf(QueueActor(3))

      val senderOne = TestProbe()
      val senderTwo = TestProbe()
      val senderThree = TestProbe()

      senderOne.send(queue, Append(Seq("one")))
      senderOne.expectNoMessage(maxWaitForMsg)

      senderTwo.send(queue, Append(Seq("two")))
      senderTwo.expectNoMessage(maxWaitForMsg)

      senderThree.send(queue, Append(Seq("three")))

      senderThree.expectMsg(maxWaitForMsg, MakeACallWith(Seq("one", "two", "three")))
      senderOne.expectMsg(maxWaitForMsg, MakeACallWith(Seq("one", "two", "three")))
      senderTwo.expectMsg(maxWaitForMsg, MakeACallWith(Seq("one", "two", "three")))

    }

    "flush the queue and send params to all subscribers when it receives a FlushQueue message" in {
      val queue = system.actorOf(QueueActor(3))

      val senderOne = TestProbe()
      val senderTwo = TestProbe()

      senderOne.send(queue, Append(Seq("one")))

      senderTwo.send(queue, Append(Seq("two")))

      senderOne.send(queue, FlushQueue)

      senderOne.expectMsg(maxWaitForMsg, MakeACallWith(Seq("one", "two")))
      senderTwo.expectMsg(maxWaitForMsg, MakeACallWith(Seq("one", "two")))
    }
  }
}
