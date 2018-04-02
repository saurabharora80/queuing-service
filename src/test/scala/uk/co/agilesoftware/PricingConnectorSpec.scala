package uk.co.agilesoftware

class PricingConnectorSpec extends ConnectorSpec {

  private val connector = new PricingConnector {
    override val serviceBaseUrl: String = wiremockUrl
  }

  "connector" should {
    val pricing = "pricing" -> "NL,CN"

    "be able to read well formatted response" in {
      given(pricing).succeedWith("""{"NL": 14.24, "CN": 20.50}""")

      whenReady(connector.get(pricing._1, pricing._2)) { _ shouldBe Map("pricing" -> Map("NL" -> 14.24, "CN" -> 20.50)) }
    }

    "return empty map if service is unreachable" in {
      given(pricing).fails

      whenReady(connector.get(pricing._1, pricing._2)) { _ shouldBe empty }
    }
  }
}
