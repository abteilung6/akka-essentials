package org.abteilung6.akka
package part3testing

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import scala.util.Random

class TimeAssertionsSpec extends TestKit(
  ActorSystem("testSystem", ConfigFactory.load().getConfig("specialTimeAssertionsConfig "))
)
  with ImplicitSender
  with AnyWordSpecLike
  with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  import TimeAssertionsSpec._

  "A work actor" should {
    val workActor = system.actorOf(Props[WorkActor], "workActor")

    "reply in a timely manner" in {
      within(500 millis, 1 second) {
        workActor ! "work"
        expectMsg(WorkResult(42))
      }
    }

    "reply with avalid work at a reasonable cadence" in {
      within(1 second) {
        workActor ! "workSequence"
        val results: Seq[Int] = receiveWhile[Int](max = 2 seconds, idle = 500 millis, messages = 10) {
          case WorkResult(result) => result
        }
        assert(results.sum > 5)
      }
    }

    "reply to a test probe in a timely manner" in {
      within(1 second) {
        val probe = TestProbe()
        probe.send(workActor, "work")
        probe.expectMsg(WorkResult(42)) // timeout of 0.8 seconds, see configuration
      }
    }
  }
}

object TimeAssertionsSpec {

  case class WorkResult(result: Int)

  class WorkActor extends Actor {
    override def receive: Receive = {
      case "work" =>
        Thread.sleep(500)
        sender() ! WorkResult(42)
      case "workSequence" =>
        val r = new Random()
        for (_ <- 1 to 10) {
          Thread.sleep(r.nextInt(50))
          sender() ! WorkResult(1)
        }
    }
  }
}