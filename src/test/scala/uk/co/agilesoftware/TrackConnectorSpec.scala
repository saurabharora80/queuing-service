package uk.co.agilesoftware

import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{Matchers, WordSpec}

class TrackConnectorSpec extends WordSpec with WiremockSpec with MockitoSugar with WiremockStub with Matchers
  with ScalaFutures with IntegrationPatience {

  private val connector = new TrackConnector {
    override val serviceBaseUrl: String = wiremockUrl
  }

  "connector" should {
    val track = "track" -> "109347263,123456891"

    "be able to read well formatted response" in {

      given(track).succeedWith("""{"109347263": "NEW", "123456891": "COLLECTING"}""")

      whenReady(connector.get(track._1, track._2)) {
        _ shouldBe Map("track" -> Map("109347263" -> "NEW", "123456891" -> "COLLECTING"))
      }
    }

    "return empty map if service is unreachable" in {
      given(track).fails

      whenReady(connector.get(track._1, track._2)) { _ shouldBe empty }
    }
  }
}
