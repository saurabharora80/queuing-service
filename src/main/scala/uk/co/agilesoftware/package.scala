package uk.co

package object agilesoftware {
  type Data = Map[String, Map[String, _]]
  type ConnectorResponse = Map[String, _]
  type ShipmentResponse = Map[String, Seq[String]]
  type TrackResponse = Map[String, String]
  type PricingResponse = Map[String, BigDecimal]
}
