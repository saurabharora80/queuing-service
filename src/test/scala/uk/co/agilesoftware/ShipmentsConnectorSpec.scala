package uk.co.agilesoftware

class ShipmentsConnectorSpec extends ConnectorSpec {

  private val connector = new ShipmentsConnector {
    override val serviceBaseUrl: String = wiremockUrl
  }

  "connector" should {
    val shipment = "shipments" -> "109347263,123456891"

    "be able to read well formatted response" in {
      given(shipment).succeedWith("""{"109347263": ["box", "box", "palet"], "123456891": ["envelope"]}""")

      whenReady(connector.get(shipment._1, shipment._2)) {
        _ shouldBe Map("shipments" -> Map("109347263" -> Seq("box", "box", "palet"), "123456891" -> Seq("envelope")))
      }
    }

    "return empty map if service is unreachable" in {
      given(shipment).fails

      whenReady(connector.get(shipment._1, shipment._2)) { _ shouldBe empty }
    }
  }
}
