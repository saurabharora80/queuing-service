package uk.co.agilesoftware

import akka.http.scaladsl.model.Uri
import akka.util.Timeout

import scala.concurrent.Future
import scala.concurrent.duration._

object Orchestrator {

  implicit lazy val timeout: Timeout = Timeout(5.seconds)

  def execute(uris: Seq[Uri]): Future[CollectedResponse] = {
    import Singletons._

    val mappedUris = uris.map(uri => uri.path.toString() -> uri).toMap

    //Define the futures outside the for yield to enable parallel execution
    val eventualShipments = ShipmentDataService.get(mappedUris)
    val eventualTrackings = TrackDataService.get(mappedUris)
    val eventualPrices = PricingDataService.get(mappedUris)

    for {
      shipments <- eventualShipments
      tracking <- eventualTrackings
      prices <- eventualPrices
    } yield shipments ++ tracking ++ prices

  }
}
