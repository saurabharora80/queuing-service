package com.lightbend.akka.sample

import akka.http.scaladsl.model.Uri
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{Matchers, WordSpec}
import com.github.tomakehurst.wiremock.client.WireMock._

class OrchestratorSpec extends WordSpec with Matchers with ScalaFutures with IntegrationPatience with WiremockSpec {

  def given = new WiremockGivens()

  "Orchestrator" should {
    def shipmentsUri(queryParams: String = "109347263,123456891") = Uri(s"$wiremockUrl/shipments?q=$queryParams")
    def trackUrl(queryParams: String = "109347263,123456891") = Uri(s"$wiremockUrl/track?q=$queryParams")
    def pricingUri(queryParams: String = "NL,CN") = Uri(s"$wiremockUrl/pricing?q=$queryParams")

    "fetch data from all 3 services" in {
      given.shipments.succeedWith("""{"109347263": ["box", "box", "palet"], "123456891": ["envelope"]}""")
      given.track.succeedWith("""{"109347263": "NEW", "123456891": "COLLECTING"}""")
      given.pricing.succeedWith("""{"NL": 14.24, "CN": 20.50}""")

      whenReady(Orchestrator.execute(Seq(shipmentsUri("109347263,123456891"),trackUrl("109347263,123456891"),pricingUri("NL,CN")))) { response =>
          response("shipments")("109347263").asInstanceOf[Seq[String]] should contain theSameElementsAs Seq("box", "box", "palet")
          response("shipments")("123456891").asInstanceOf[Seq[String]] should contain theSameElementsAs Seq("envelope")

          response("track")("109347263").asInstanceOf[String] shouldBe "NEW"
          response("track")("123456891").asInstanceOf[String] shouldBe "COLLECTING"

          response("pricing")("NL").asInstanceOf[BigDecimal] shouldBe 14.24
          response("pricing")("CN").asInstanceOf[BigDecimal] shouldBe 20.50
      }
    }

    "fetch data for shipments and track if pricing is unreachable" in {

      given.shipments.succeeds
      given.track.succeeds
      given.pricing.fails

      whenReady(Orchestrator.execute(Seq(shipmentsUri(),trackUrl(),pricingUri()))) { response =>
        response.contains("shipments") shouldBe true
        response.contains("track") shouldBe true
        response.contains("pricing") shouldBe false
      }
    }

    "fetch data for pricing and track if shipments is unreachable" in {
      given.shipments.fails
      given.track.succeeds
      given.pricing.succeeds

      whenReady(Orchestrator.execute(Seq(shipmentsUri(),trackUrl(),pricingUri()))) { response =>
        response.contains("shipments") shouldBe false
        response.contains("track") shouldBe true
        response.contains("pricing") shouldBe true
      }
    }

    "fetch data for pricing and shipment if track is unreachable" in {
      given.shipments.succeeds
      given.track.fails
      given.pricing.succeeds

      whenReady(Orchestrator.execute(Seq(shipmentsUri(),trackUrl(),pricingUri()))) { response =>
        response.contains("shipments") shouldBe true
        response.contains("track") shouldBe false
        response.contains("pricing") shouldBe true
      }
    }
  }

}

class WiremockGivens() {
  def pricing = new NestedGivens("pricing")

  def track = new NestedGivens("track")

  def shipments = new NestedGivens("shipments")

  class NestedGivens(path: String) {
    def fails: Unit = stubFor(get(urlPathMatching(s"/$path.*")).willReturn(aResponse().withStatus(503)))

    def succeeds: Unit = {
      path match {
        case "shipments" => succeedWith("""{"109347263": ["box", "box", "palet"], "123456891": ["envelope"]}""")
        case "track" => succeedWith("""{"109347263": "NEW", "123456891": "COLLECTING"}""")
        case "pricing" => succeedWith("""{"NL": 14.24, "CN": 20.50}""")
      }
    }

    def succeedWith(jsonBody: String): Unit =
      stubFor(get(urlPathMatching(s"/$path.*")).willReturn(
        aResponse()
          .withStatus(200)
          .withHeader("Content-Type", "application/json")
          .withBody(jsonBody)))
  }

}


