package com.lightbend.akka.sample

import java.net.URL

import akka.actor.{Actor, ActorLogging, Props}

object ShipmentsActor {
  final case class Get(url: URL)

  def props: Props = Props[ShipmentsActor]
}

class ShipmentsActor extends Actor with ActorLogging {
  import ShipmentsActor._

  var urls = Set.empty[URL]

  def receive: Receive = {
    case Get(url) =>
      urls += url
      sender() ! Map("shipments" -> Map("109347263" -> Seq("box", "box", "palet"), "123456891" -> Seq("envelope")))
  }
}
