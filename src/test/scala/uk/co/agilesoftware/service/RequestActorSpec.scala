package uk.co.agilesoftware.service

import akka.pattern.ask
import akka.util.Timeout
import org.mockito.BDDMockito.given
import org.mockito.Mockito
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.{Eventually, IntegrationPatience, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import uk.co.agilesoftware.ConnectorResponse
import uk.co.agilesoftware.connector.DownstreamConnector
import uk.co.agilesoftware.service.RequestActor.{GetResponse, RequestFor}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

class RequestActorSpec extends ActorSpec("RequestActorSpec") with Eventually with ScalaFutures with MockitoSugar
  with IntegrationPatience with BeforeAndAfterEach {

  implicit lazy val timeout: Timeout = Timeout(5.seconds)

  val apiConnector: DownstreamConnector = mock[DownstreamConnector]

  override def beforeEach(): Unit = {
    super.beforeEach()
    Mockito.reset(apiConnector)
  }

   "request" should {

     "not be made if the queue doesn't get full" in {
       val queueActor = system.actorOf(QueueActor(maxQueueSize = 3))

       val requestOne = system.actorOf(RequestActor(queueActor, apiConnector))
       val requestTwo = system.actorOf(RequestActor(queueActor, apiConnector))

       requestOne ! RequestFor(Seq("one"))
       requestTwo ! RequestFor(Seq("two"))

       eventually {
         whenReady(requestOne ? GetResponse) {
           _.asInstanceOf[Option[ConnectorResponse]] shouldBe empty
         }
       }

       eventually {
         whenReady(requestTwo ? GetResponse) {
           _.asInstanceOf[Option[ConnectorResponse]] shouldBe empty
         }
       }

       verify(apiConnector, never()).get(Seq("one", "two"))
     }

     "be made on when the queue gets full" in {

       val queueActor = system.actorOf(QueueActor(maxQueueSize = 3))

       val requestOne = system.actorOf(RequestActor(queueActor, apiConnector))
       val requestTwo = system.actorOf(RequestActor(queueActor, apiConnector))

       given(apiConnector.get(Seq("one", "two", "three")))
         .willReturn(Future(Map("one" -> "valueOne", "two" -> "valueTwo", "three" -> "valueThree")))

       requestOne ! RequestFor(Seq("one", "two"))

       requestTwo ! RequestFor(Seq("three"))

       eventually {
         whenReady(requestOne ? GetResponse) {
           _.asInstanceOf[Option[ConnectorResponse]] shouldBe Some(Map("one" -> "valueOne", "two" -> "valueTwo"))
         }
       }

       eventually {
         whenReady(requestTwo ? GetResponse) {
           _.asInstanceOf[Option[ConnectorResponse]] shouldBe Some(Map("three" -> "valueThree"))
         }
       }
     }

     "be made if the queue fills up on the first request" in {

       val queueActor = system.actorOf(QueueActor(maxQueueSize = 3))
       val requestOne = system.actorOf(RequestActor(queueActor, apiConnector))

       given(apiConnector.get(Seq("one", "two", "three")))
         .willReturn(Future(Map("one" -> "valueOne", "two" -> "valueTwo", "three" -> "valueThree")))

       requestOne ! RequestFor(Seq("one", "two", "three"))

       eventually {
         whenReady(requestOne ? GetResponse) {
           _.asInstanceOf[Option[ConnectorResponse]] shouldBe Some(Map("one" -> "valueOne", "two" -> "valueTwo", "three" -> "valueThree"))
         }
       }
     }

     "be made within 1 second even if queue is not full" in {

       val queueActor = system.actorOf(QueueActor(maxQueueSize = 3))

       val requestOne = system.actorOf(RequestActor(queueActor, apiConnector, 500.milliseconds))
       val requestTwo = system.actorOf(RequestActor(queueActor, apiConnector, 500.milliseconds))

       given(apiConnector.get(Seq("one", "two"))).willReturn(Future(Map("one" -> "valueOne", "two" -> "valueTwo")))

       requestOne ! RequestFor(Seq("one"))

       requestTwo ! RequestFor(Seq("two"))

       eventually {
         whenReady(requestOne ? GetResponse) {
           _.asInstanceOf[Option[ConnectorResponse]] shouldBe Some(Map("one" -> "valueOne"))
         }
       }

       eventually {
         whenReady(requestTwo ? GetResponse) {
           _.asInstanceOf[Option[ConnectorResponse]] shouldBe Some(Map("two" -> "valueTwo"))
         }
       }
     }
   }
}









