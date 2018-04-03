package uk.co.agilesoftware

import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{Matchers, WordSpec}

trait ConnectorSpec extends WordSpec with WiremockSpec with WiremockStub with Matchers with ScalaFutures
  with IntegrationPatience {
  implicit def stringToList(params: String) = params.split(",").toSeq
}
