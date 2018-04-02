package uk.co.agilesoftware

import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{Matchers, WordSpec}

class OrchestratorSpec extends WordSpec with Matchers with ScalaFutures with IntegrationPatience with WiremockSpec with WiremockStub {

  private val orchestrator: Orchestrator = new Orchestrator {
    override val shipmentDataService: DataService = new ShipmentDataService {
      override protected def connector: DownstreamConnector = new ShipmentsConnector {
        override val serviceBaseUrl: String = wiremockUrl
      }
    }
    override val trackDataService: DataService = new TrackDataService {
      override protected def connector: DownstreamConnector = new TrackConnector {
        override val serviceBaseUrl: String = wiremockUrl
      }
    }
    override val pricingDataService: DataService = new PricingDataService {
      override protected def connector: DownstreamConnector = new PricingConnector {
        override val serviceBaseUrl: String = wiremockUrl
      }
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

    "fetch data for shipments and track if pricing is unreachable" in {

      given(shipment).succeeds
      given(track).succeeds
      given(pricing).fails

      whenReady(orchestrator.execute(Seq(shipment,track, pricing).toMap)) { response =>
        response.contains("shipments") shouldBe true
        response.contains("track") shouldBe true
        response.contains("pricing") shouldBe false
      }
    }

    "fetch data for pricing and track if shipments is unreachable" in {
      given(shipment).fails
      given(track).succeeds
      given(pricing).succeeds

      whenReady(orchestrator.execute(Seq(shipment,track, pricing).toMap)) { response =>
        response.contains("shipments") shouldBe false
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
        response.contains("track") shouldBe false
        response.contains("pricing") shouldBe true
      }
    }
  }


}


