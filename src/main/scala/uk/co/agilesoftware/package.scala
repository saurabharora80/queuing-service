package uk.co

package object agilesoftware {
  type CollectedResponse = Map[String, Map[String, _]]
  type ApiResponse = Map[String, _]
  type ShipmentResponse = Map[String, Seq[String]]
  type TrackResponse = Map[String, String]
  type PricingResponse = Map[String, BigDecimal]
}
