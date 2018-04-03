package uk.co.agilesoftware.service

import akka.actor.{ActorRef, ActorSystem}
import org.mockito.BDDMockito._
import org.mockito.Mockito
import org.scalatest.concurrent.{IntegrationPatience, PatienceConfiguration, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Matchers, WordSpec}
import uk.co.agilesoftware.connector.DownstreamConnector

import scala.concurrent.Future
import scala.concurrent.duration._

class DataServiceSpec extends WordSpec with Matchers with ScalaFutures with IntegrationPatience with BeforeAndAfterAll
  with MockitoSugar with BeforeAndAfterEach {

  private implicit val system: ActorSystem = ActorSystem("test-actor-system")

  private val mockConnector = mock[DownstreamConnector]

  override def beforeEach(): Unit = {
    super.beforeEach()
    Mockito.reset(mockConnector)
  }

  class TestDataService(override val queue: ActorRef, override val forcePullResponseIn: FiniteDuration = 2.seconds) extends DataService {
    override protected def connector: DownstreamConnector = mockConnector
    override val name: String = "shipments"
  }

  "service" should {
    "get data if a single receives capped number of params (which is 2 as per config above)" in
      new TestDataService(system.actorOf(QueueActor(maxQueueSize = 2))) {

        val params = Seq("one", "two")

      given(mockConnector.get(params))
        .willReturn(Future.successful(Map("one" -> "valueOne", "two" -> "valueTwo")))

      whenReady(get(params)) { data =>
        data shouldBe Map("shipments" -> Map("one" -> "valueOne", "two" -> "valueTwo"))
      }
    }

    "force pull data from backend after 1" in
      new TestDataService(system.actorOf(QueueActor(maxQueueSize = 2)), 1.second){

        val params = Seq("one")

      given(mockConnector.get(params))
        .willReturn(Future.successful(Map("one" -> "valueOne")))

      whenReady(get(params), PatienceConfiguration.Timeout(2.seconds)) { data =>
        data shouldBe Map("shipments" -> Map("one" -> "valueOne"))
      }
    }

    "get data when multiple calls receives capped number of params (which is 2 as per config above)" ignore
      new TestDataService(system.actorOf(QueueActor(maxQueueSize = 2))) {

      given(mockConnector.get(Seq("one", "two")))
        .willReturn(Future.successful(Map("one" -> "valueOne", "two" -> "valueTwo")))

      whenReady(get(Seq("one"))) { data =>
        data shouldBe Map("shipments" -> Map("one" -> "valueOne"))
      }

      whenReady(get(Seq("two"))) { data =>
        data shouldBe Map("shipments" -> Map("two" -> "valueTwo"))
      }
    }

  }
}
