package uk.co.agilesoftware

import akka.util.Timeout

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

trait Orchestrator {

  implicit lazy val timeout: Timeout = Timeout(5.seconds)
  val shipmentDataService: DataService
  val trackDataService: DataService
  val pricingDataService: DataService

  class DataServiceWrapper(dataService: DataService) {
    def mayBeItems(implicit serviceParams: Map[String, String], ec: ExecutionContext): Future[CollectedResponse] = {
      serviceParams.get(dataService.name) match {
        case Some(params) => dataService.get(params.split(",").toList)
        case None => Future(Map.empty)
      }
    }
  }

  implicit def dataServiceWrapper(dataService: DataService) = new DataServiceWrapper(dataService)

  def execute(implicit serviceParams: Map[String, String]): Future[CollectedResponse] = {
    import Singletons._

    //Define the futures outside the for yield to enable parallel execution
    val eventualShipments = shipmentDataService.mayBeItems
    val eventualTrackings = trackDataService.mayBeItems
    val eventualPrices = pricingDataService.mayBeItems

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

