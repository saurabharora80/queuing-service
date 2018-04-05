package uk.co.agilesoftware

import akka.actor.{ActorRef, ActorSystem}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}
import uk.co.agilesoftware.connector.{DownstreamConnector, PricingConnector, ShipmentsConnector, TrackConnector}
import uk.co.agilesoftware.service._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.Await

class OrchestratorSpec extends WordSpec with Matchers with ScalaFutures with IntegrationPatience with WiremockSpec
  with WiremockStub with BeforeAndAfterAll {
  private implicit val system: ActorSystem = ActorSystem("test-actor-system")

  override def afterAll(): Unit = {
    Await.result(system.terminate(), 500 milliseconds)
  }

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = scaled(Span(7, Seconds)),
    interval = scaled(Span(150, Millis))
  )

  class TestOrchestrator(shipmentsQueue: ActorRef = system.actorOf(QueueActor(2))) extends Orchestrator {
    override val shipmentDataService: DataService = new ShipmentDataService {
      override protected def connector: DownstreamConnector = new ShipmentsConnector {
        override val serviceBaseUrl: String = wiremockUrl
        override val name: String = "shipments"

      }
      override protected def queue: ActorRef = shipmentsQueue
    }
    override val trackDataService: DataService = new TrackDataService {
      override protected def connector: DownstreamConnector = new TrackConnector {
        override val serviceBaseUrl: String = wiremockUrl
        override val name: String = "track"
      }
      override protected def queue: ActorRef = system.actorOf(QueueActor(2))
    }
    override val pricingDataService: DataService = new PricingDataService {
      override protected def connector: DownstreamConnector = new PricingConnector {
        override val serviceBaseUrl: String = wiremockUrl
        override val name: String = "pricing"
      }
      override protected def queue: ActorRef = system.actorOf(QueueActor(2))
    }
  }

  "Orchestrator" should {
    val shipment = "shipments" -> "109347263,123456891"
    val track = "track" -> "109347263,123456891"
    val pricing = "pricing" -> "NL,CN"

    "fetch data from all 3 services" in new TestOrchestrator {
      given(shipment).succeedWith("""{"109347263": ["box", "box", "palet"], "123456891": ["envelope"]}""")
      given(track).succeedWith("""{"109347263": "NEW", "123456891": "COLLECTING"}""")
      given(pricing).succeedWith("""{"NL": 14.24, "CN": 20.50}""")

      whenReady(execute(Seq(shipment,track, pricing).toMap)) { response =>
          response("shipments")("109347263").asInstanceOf[Seq[String]] should contain theSameElementsAs Seq("box", "box", "palet")
          response("shipments")("123456891").asInstanceOf[Seq[String]] should contain theSameElementsAs Seq("envelope")

          response("track")("109347263").asInstanceOf[String] shouldBe "NEW"
          response("track")("123456891").asInstanceOf[String] shouldBe "COLLECTING"

          response("pricing")("NL").asInstanceOf[BigDecimal] shouldBe 14.24
          response("pricing")("CN").asInstanceOf[BigDecimal] shouldBe 20.50
      }
    }

    "ignore duplicate params" in new TestOrchestrator {
      val shipmentsWithDuplicateParam = "shipments" -> "109347263,123456891,123456891"

      given("shipments" -> "109347263,123456891").succeedWith("""{"109347263": ["box", "box", "palet"], "123456891": ["envelope"]}""")

      whenReady(execute(Seq(shipmentsWithDuplicateParam).toMap)) { response =>
        response("shipments")("109347263").asInstanceOf[Seq[String]] should contain theSameElementsAs Seq("box", "box", "palet")
        response("shipments")("123456891").asInstanceOf[Seq[String]] should contain theSameElementsAs Seq("envelope")
      }
    }

    "be able to make parallel calls each with capped params list" in new TestOrchestrator {
      private val anotherShipmentReq = "shipments" -> "109347264,123456892"

      given(shipment).succeedWith("""{"109347263": ["box", "box", "palet"], "123456891": ["envelope"]}""")
      given(anotherShipmentReq).succeedWith("""{"109347264": ["box", "box", "palet"], "123456892": ["envelope"]}""")

      private val eventualResponseOne = execute(Seq(shipment).toMap)
      private val eventualResponseTwo = execute(Seq(anotherShipmentReq).toMap)

      whenReady(for {
        responseOne <- eventualResponseOne
        responseTwo <- eventualResponseTwo
      } yield (responseOne, responseTwo)) {
        case (responseOne, responseTwo) =>
          responseOne("shipments")("109347263").asInstanceOf[Seq[String]] should contain theSameElementsAs Seq("box", "box", "palet")
          responseOne("shipments")("123456891").asInstanceOf[Seq[String]] should contain theSameElementsAs Seq("envelope")

          responseTwo("shipments")("109347264").asInstanceOf[Seq[String]] should contain theSameElementsAs Seq("box", "box", "palet")
          responseTwo("shipments")("123456892").asInstanceOf[Seq[String]] should contain theSameElementsAs Seq("envelope")
      }
    }

    "be able to make parallel calls each with partial params list" in
      new TestOrchestrator(system.actorOf(QueueActor(2))) {
      given("shipments" -> "109347263,123456891").succeedWith("""{"109347263": ["box", "box", "palet"], "123456891": ["envelope"]}""")

      private val eventualResponseOne = execute(Seq("shipments" -> "109347263").toMap)
      private val eventualResponseTwo = execute(Seq("shipments" -> "123456891").toMap)

      whenReady(for {
          responseOne <- eventualResponseOne
          responseTwo <- eventualResponseTwo
        } yield (responseOne, responseTwo)) {
        case (responseOne, responseTwo) =>
          responseOne("shipments")("109347263").asInstanceOf[Seq[String]] should contain theSameElementsAs Seq("box", "box", "palet")
          responseTwo("shipments")("123456891").asInstanceOf[Seq[String]] should contain theSameElementsAs Seq("envelope")
      }
    }

    "fetch data for shipments and track if pricing is unreachable" in new TestOrchestrator {

      given(shipment).succeeds
      given(track).succeeds
      given(pricing).fails

      whenReady(execute(Seq(shipment,track, pricing).toMap)) { response =>
        response.contains("shipments") shouldBe true
        response.contains("track") shouldBe true
        response.get("pricing") shouldBe Some(Map())
      }
    }

    "fetch data for pricing and track if shipments is unreachable" in new TestOrchestrator {
      given(shipment).fails
      given(track).succeeds
      given(pricing).succeeds

      whenReady(execute(Seq(shipment,track, pricing).toMap)) { response =>
        response.get("shipments") shouldBe Some(Map())
        response.contains("track") shouldBe true
        response.contains("pricing") shouldBe true
      }
    }

    "fetch data for pricing and shipment if track is unreachable" in new TestOrchestrator {
      given(shipment).succeeds
      given(track).fails
      given(pricing).succeeds

      whenReady(execute(Seq(shipment,track, pricing).toMap)) { response =>
        response.contains("shipments") shouldBe true
        response.get("track") shouldBe Some(Map())
        response.contains("pricing") shouldBe true
      }
    }
  }


}


