package uk.co.agilesoftware

import akka.util.Timeout
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import uk.co.agilesoftware.RequestActor._
import akka.pattern.ask
import org.mockito.BDDMockito.given
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

class RequestActorSpec extends ActorSpec("RequestActorSpec") with Eventually with ScalaFutures with MockitoSugar {

  implicit lazy val timeout: Timeout = Timeout(5.seconds)

  val apiConnector: ApiConnector = mock[ApiConnector]

   "request" should {

     "not be made if the queue doesn't get full" in {
       val queueActor = system.actorOf(QueueActor.props(3))

       val requestOne = system.actorOf(RequestActor(queueActor, apiConnector))
       val requestTwo = system.actorOf(RequestActor(queueActor, apiConnector))

       requestOne ! RequestFor(Seq("one"))
       requestTwo ! RequestFor(Seq("two"))

       eventually {
         whenReady(requestOne ? GetResponse) {
           _.asInstanceOf[Option[ApiResponse]] shouldBe empty
         }
       }

       eventually {
         whenReady(requestTwo ? GetResponse) {
           _.asInstanceOf[Option[ApiResponse]] shouldBe empty
         }
       }

       verify(apiConnector, never()).get(Seq("one", "two"))
     }

     "be made on when the queue gets full" in {

       val queueActor = system.actorOf(QueueActor.props(3))

       val requestOne = system.actorOf(RequestActor(queueActor, apiConnector))
       val requestTwo = system.actorOf(RequestActor(queueActor, apiConnector))

       given(apiConnector.get(Seq("one", "two", "three")))
         .willReturn(Future(Map("one" -> "valueOne", "two" -> "valueTwo", "three" -> "valueThree")))

       requestOne ! RequestFor(Seq("one", "two"))

       requestTwo ! RequestFor(Seq("three"))

       eventually {
         whenReady(requestOne ? GetResponse) {
           _.asInstanceOf[Option[ApiResponse]] shouldBe Some(Map("one" -> "valueOne", "two" -> "valueTwo"))
         }
       }

       eventually {
         whenReady(requestTwo ? GetResponse) {
           _.asInstanceOf[Option[ApiResponse]] shouldBe Some(Map("three" -> "valueThree"))
         }
       }
     }

     "be made if the queue fills up on the first request" in {

       val queueActor = system.actorOf(QueueActor.props(3))
       val requestOne = system.actorOf(RequestActor(queueActor, apiConnector))

       given(apiConnector.get(Seq("one", "two", "three")))
         .willReturn(Future(Map("one" -> "valueOne", "two" -> "valueTwo", "three" -> "valueThree")))

       requestOne ! RequestFor(Seq("one", "two", "three"))

       eventually {
         whenReady(requestOne ? GetResponse) {
           _.asInstanceOf[Option[ApiResponse]] shouldBe Some(Map("one" -> "valueOne", "two" -> "valueTwo", "three" -> "valueThree"))
         }
       }
     }
   }
}









