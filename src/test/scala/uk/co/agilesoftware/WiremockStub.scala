package uk.co.agilesoftware

import com.github.tomakehurst.wiremock.client.WireMock._

trait WiremockStub {

  protected def given(pathWithParams: (String, String)) = new Wiremock(pathWithParams._1, pathWithParams._2)

  class Wiremock(path: String, params: String) {
    def fails: Unit = stubFor(get(urlPathEqualTo(s"/$path")).willReturn(aResponse().withStatus(503)))

    def succeeds: Unit = {
      path match {
        case "shipments" => succeedWith("""{"109347263": ["box", "box", "palet"], "123456891": ["envelope"]}""")
        case "track" => succeedWith("""{"109347263": "NEW", "123456891": "COLLECTING"}""")
        case "pricing" => succeedWith("""{"NL": 14.24, "CN": 20.50}""")
      }
    }

    def succeedWith(jsonBody: String): Unit = {
      stubFor(get(urlPathEqualTo(s"/$path")).withQueryParam("q", equalTo(params))
        .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(jsonBody)))
    }
  }
}
