package uk.co.agilesoftware

import scala.concurrent.{ExecutionContext, Future}

trait ApiConnector {
  def get(params: Seq[String])(implicit ec: ExecutionContext): Future[ConnectorResponse] = Future { Map("" -> "")}

}
