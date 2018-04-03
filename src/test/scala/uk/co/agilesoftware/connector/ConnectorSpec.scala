package uk.co.agilesoftware.connector

import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{Matchers, WordSpec}
import uk.co.agilesoftware.{WiremockSpec, WiremockStub}

trait ConnectorSpec extends WordSpec with WiremockSpec with WiremockStub with Matchers with ScalaFutures
  with IntegrationPatience {
  implicit def stringToList(params: String) = params.split(",").toSeq
}
