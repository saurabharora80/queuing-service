package uk.co.agilesoftware.connector

class PricingConnectorSpec extends ConnectorSpec {

  private val connector = new PricingConnector {
    override val serviceBaseUrl: String = wiremockUrl
    override val name: String = "pricing"
  }

  "connector" should {
    val pricing = "pricing" -> "NL,CN"

    "be able to read well formatted response" in {
      given(pricing).succeedWith("""{"NL": 14.24, "CN": 20.50}""")

      whenReady(connector.get(pricing._2)) { _ shouldBe Map("NL" -> 14.24, "CN" -> 20.50) }
    }

    "return empty map if service is unreachable" in {
      given(pricing).fails

      whenReady(connector.get(pricing._2)) { _ shouldBe empty }
    }
  }
}
