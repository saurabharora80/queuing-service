package com.lightbend.akka.sample

import java.net.URL

import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{Matchers, WordSpec}

class OrchestratorSpec extends WordSpec with Matchers with ScalaFutures with IntegrationPatience {

  "" should {
    "" in {
      whenReady(Orchestrator.execute(Seq(
        new URL("http://domain.com/shipments?q=109347263,123456891"),
        new URL("http://domain.com/track?q=109347263,123456891"),
        new URL("http://domain.com/pricings?q=NL,CN")
      ))) { response =>
          response("shipments")("109347263").asInstanceOf[Seq[String]] should contain theSameElementsAs Seq("box", "box", "palet")
          response("shipments")("123456891").asInstanceOf[Seq[String]] should contain theSameElementsAs Seq("envelope")

          /*response("track")("109347263").asInstanceOf[String] shouldBe "NEW"
          response("track")("123456891").asInstanceOf[String] shouldBe "COLLECTING"

          response("pricings")("NL").asInstanceOf[BigDecimal] shouldBe "14.24"
          response("pricings")("CN").asInstanceOf[BigDecimal] shouldBe "20.50"*/
      }
    }
  }

}


