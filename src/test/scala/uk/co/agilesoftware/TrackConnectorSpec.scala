package uk.co.agilesoftware

class TrackConnectorSpec extends ConnectorSpec {

  private val connector = new TrackConnector {
    override val serviceBaseUrl: String = wiremockUrl
    override val name: String = "track"
  }

  "connector" should {
    val track = "track" -> "109347263,123456891"

    "be able to read well formatted response" in {

      given(track).succeedWith("""{"109347263": "NEW", "123456891": "COLLECTING"}""")

      whenReady(connector.get(track._2)) {
        _ shouldBe Map("109347263" -> "NEW", "123456891" -> "COLLECTING")
      }
    }

    "return empty map if service is unreachable" in {
      given(track).fails

      whenReady(connector.get(track._2)) { _ shouldBe empty }
    }
  }
}
