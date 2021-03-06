package uk.co.agilesoftware

import akka.util.Timeout
import uk.co.agilesoftware.service.{DataService, PricingDataService, ShipmentDataService, TrackDataService}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

trait Orchestrator {

  implicit lazy val timeout: Timeout = Timeout(5.seconds)
  val shipmentDataService: DataService
  val trackDataService: DataService
  val pricingDataService: DataService

  implicit class DataServiceWrapper(dataService: DataService) {
    def eventualData(implicit serviceParams: Map[String, String], ec: ExecutionContext): Future[Data] = {
      serviceParams.get(dataService.name) match {
        case Some(params) => dataService.get(params.split(",").toSeq.distinct)
        case None => Future(Map.empty)
      }
    }
  }

  def execute(implicit serviceParams: Map[String, String]): Future[Data] = {
    import Singletons._

    //Define the futures outside the for yield to enable parallel execution
    val eventualShipments = shipmentDataService.eventualData
    val eventualTrackings = trackDataService.eventualData
    val eventualPrices = pricingDataService.eventualData

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

