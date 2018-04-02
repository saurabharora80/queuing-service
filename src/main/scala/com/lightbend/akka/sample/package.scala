package com.lightbend.akka

package object sample {
  type CollectedResponse = Map[String, Map[String, _]]
  type ShipmentResponse = Map[String, Seq[String]]
  type TrackResponse = Map[String, String]
  type PricingResponse = Map[String, BigDecimal]
}
