package uk.co.agilesoftware

import akka.util.Timeout

import scala.concurrent.Future
import scala.concurrent.duration._

trait Orchestrator {

  implicit lazy val timeout: Timeout = Timeout(5.seconds)
  val shipmentDataService: DataService
  val trackDataService: DataService
  val pricingDataService: DataService


  def execute(serviceParams: Map[String, String]): Future[CollectedResponse] = {
    import Singletons._

    //Define the futures outside the for yield to enable parallel execution
    val eventualShipments = shipmentDataService.get(serviceParams)
    val eventualTrackings = trackDataService.get(serviceParams)
    val eventualPrices = pricingDataService.get(serviceParams)

    for {
      shipments <- eventualShipments
      tracking <- eventualTrackings
      prices <- eventualPrices
    } yield shipments ++ tracking ++ prices

  }
}

object Orchestrator extends Orchestrator {
  override val shipmentDataService: DataService = ShipmentDataService
  override val trackDataService: DataService = TrackDataService
  override val pricingDataService: DataService = PricingDataService
}