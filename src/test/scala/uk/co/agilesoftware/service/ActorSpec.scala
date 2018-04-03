package uk.co.agilesoftware.service

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration._

abstract class ActorSpec(name: String) extends TestKit(ActorSystem(name)) with WordSpecLike with Matchers
  with BeforeAndAfterAll with ImplicitSender {
  override def afterAll: Unit = {
    shutdown(system)
  }

  protected val maxWaitForMsg = 300 millis

}
