package uk.co.agilesoftware

import akka.actor.{ActorRef, ActorSystem}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}
import scala.concurrent.duration._
import scala.concurrent.Await

class OrchestratorSpec extends WordSpec with Matchers with ScalaFutures with IntegrationPatience with WiremockSpec
  with WiremockStub with BeforeAndAfterAll {
  private implicit val system: ActorSystem = ActorSystem("test-actor-system")

  override def afterAll(): Unit = {
    Await.result(system.terminate(), 500 milliseconds)
  }

  private val orchestrator: Orchestrator = new Orchestrator {
    override val shipmentDataService: DataService = new ShipmentDataService {
      override protected def connector: DownstreamConnector = new ShipmentsConnector {
        override val serviceBaseUrl: String = wiremockUrl
        override val name: String = "shipments"
      }

      override protected def queue: ActorRef = system.actorOf(QueueActor(2))
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

    "fetch data from all 3 services" in {
      given(shipment).succeedWith("""{"109347263": ["box", "box", "palet"], "123456891": ["envelope"]}""")
      given(track).succeedWith("""{"109347263": "NEW", "123456891": "COLLECTING"}""")
      given(pricing).succeedWith("""{"NL": 14.24, "CN": 20.50}""")

      whenReady(orchestrator.execute(Seq(shipment,track, pricing).toMap)) { response =>
          response("shipments")("109347263").asInstanceOf[Seq[String]] should contain theSameElementsAs Seq("box", "box", "palet")
          response("shipments")("123456891").asInstanceOf[Seq[String]] should contain theSameElementsAs Seq("envelope")

          response("track")("109347263").asInstanceOf[String] shouldBe "NEW"
          response("track")("123456891").asInstanceOf[String] shouldBe "COLLECTING"

          response("pricing")("NL").asInstanceOf[BigDecimal] shouldBe 14.24
          response("pricing")("CN").asInstanceOf[BigDecimal] shouldBe 20.50
      }
    }

    "ignore duplicate params" in {
      val shipmentsWithDuplicateParam = "shipments" -> "109347263,123456891,123456891"

      given("shipments" -> "109347263,123456891").succeedWith("""{"109347263": ["box", "box", "palet"], "123456891": ["envelope"]}""")

      whenReady(orchestrator.execute(Seq(shipmentsWithDuplicateParam).toMap)) { response =>
        response("shipments")("109347263").asInstanceOf[Seq[String]] should contain theSameElementsAs Seq("box", "box", "palet")
        response("shipments")("123456891").asInstanceOf[Seq[String]] should contain theSameElementsAs Seq("envelope")
      }
    }

    "be able to make multiple calls" in {
      val anotherShipmentReq = "shipments" -> "109347264,123456892"

      given(shipment).succeedWith("""{"109347263": ["box", "box", "palet"], "123456891": ["envelope"]}""")
      given(anotherShipmentReq).succeedWith("""{"109347264": ["box", "box", "palet"], "123456892": ["envelope"]}""")

      whenReady(orchestrator.execute(Seq(shipment).toMap)) { response =>
        response("shipments")("109347263").asInstanceOf[Seq[String]] should contain theSameElementsAs Seq("box", "box", "palet")
        response("shipments")("123456891").asInstanceOf[Seq[String]] should contain theSameElementsAs Seq("envelope")
      }

      whenReady(orchestrator.execute(Seq(anotherShipmentReq).toMap)) { response =>
        response("shipments")("109347264").asInstanceOf[Seq[String]] should contain theSameElementsAs Seq("box", "box", "palet")
        response("shipments")("123456892").asInstanceOf[Seq[String]] should contain theSameElementsAs Seq("envelope")
      }
    }

    "be able to make multiple calls with partial params list" ignore {
      val anotherShipment = "shipments" -> "109347263"
      val yetAnotherShipment = "shipments" -> "123456891"

      given("shipments" -> "109347263,123456891").succeedWith("""{"109347263": ["box", "box", "palet"], "123456891": ["envelope"]}""")

      whenReady(orchestrator.execute(Seq(anotherShipment).toMap)) { response =>
        response("shipments")("109347263").asInstanceOf[Seq[String]] should contain theSameElementsAs Seq("box", "box", "palet")
      }

      whenReady(orchestrator.execute(Seq(yetAnotherShipment).toMap)) { response =>
        response("shipments")("123456891").asInstanceOf[Seq[String]] should contain theSameElementsAs Seq("envelope")
      }
    }

    "fetch data for shipments and track if pricing is unreachable" in {

      given(shipment).succeeds
      given(track).succeeds
      given(pricing).fails

      whenReady(orchestrator.execute(Seq(shipment,track, pricing).toMap)) { response =>
        response.contains("shipments") shouldBe true
        response.contains("track") shouldBe true
        response.get("pricing") shouldBe Some(Map())
      }
    }

    "fetch data for pricing and track if shipments is unreachable" in {
      given(shipment).fails
      given(track).succeeds
      given(pricing).succeeds

      whenReady(orchestrator.execute(Seq(shipment,track, pricing).toMap)) { response =>
        response.get("shipments") shouldBe Some(Map())
        response.contains("track") shouldBe true
        response.contains("pricing") shouldBe true
      }
    }

    "fetch data for pricing and shipment if track is unreachable" in {
      given(shipment).succeeds
      given(track).fails
      given(pricing).succeeds

      whenReady(orchestrator.execute(Seq(shipment,track, pricing).toMap)) { response =>
        response.contains("shipments") shouldBe true
        response.get("track") shouldBe Some(Map())
        response.contains("pricing") shouldBe true
      }
    }
  }


}


