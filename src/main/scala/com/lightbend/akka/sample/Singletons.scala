package com.lightbend.akka.sample

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

import scala.concurrent.ExecutionContext

object Singletons {
  implicit val system: ActorSystem = ActorSystem("helloAkka")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher
}
