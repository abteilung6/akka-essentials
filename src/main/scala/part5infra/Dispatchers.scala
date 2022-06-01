package org.abteilung6.akka
package part5infra

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import com.typesafe.config.ConfigFactory

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.Random

object Dispatchers extends App {

  class Counter extends Actor with ActorLogging {
    var count = 0

    override def receive: Receive = {
      case message =>
        count += 1
        log.info(s"[$count] $message")
    }
  }

  val system = ActorSystem("DispatchersDemo")

  // method: programmatic
  val actors = for(i <- 1 to 10)
    yield system.actorOf(Props[Counter].withDispatcher("my-dispatcher"), s"counter_$i")
  val r = new Random()
  for (i  <- 1 to 1000) {
    //actors(r.nextInt(10)) ! i
  }

  // method: from config
  //val rtjvmActor = system.actorOf(Props[Counter], "rtjvm")


  class DBActor extends Actor with ActorLogging {
    // implicit val executionContext: ExecutionContextExecutor = context.dispatcher
    // solution 1
    implicit val executionContext: ExecutionContextExecutor = context.system.dispatchers.lookup("my-dispatcher")


    override def receive: Receive = {
      case message => Future {
        Thread.sleep(500)
        log.info(message.toString)
      }
    }
  }

  val dbActor = system.actorOf(Props[DBActor], "dbActor")
  // dbActor ! "the meaning of life is 42"

  val nonblockingActor = system.actorOf(Props[Counter])
  for (i <- 1 to 1000) {
    val message = s"important message $i"
    dbActor ! message
    nonblockingActor ! message
  }
}
