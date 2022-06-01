package org.abteilung6.akka
package part5infra

import akka.actor.{Actor, ActorLogging, ActorSystem, Props, Terminated}
import akka.routing.{ActorRefRoutee, Broadcast, FromConfig, RoundRobinGroup, RoundRobinPool, RoundRobinRoutingLogic, Router}
import com.typesafe.config.ConfigFactory

object Routers extends App {

  /**
   * 1 - manual router
   */
  class Master extends Actor {
    // step 1 - create routees
    private val slaves = for (i <- 1 to 5) yield {
      val slave = context.actorOf(Props[Slave], s"slave_$i")
      context.watch(slave)

      ActorRefRoutee(slave)
    }

    // step 2 - define router
    private var router = Router(RoundRobinRoutingLogic(), slaves)
    // round robin, random, smallest inbox, broadcast, scatter-gather-first, tail-chopping
    // consistent-hashing

    override def receive: Receive = {
      // step 4 - handle termination/lifecycle of the routees
      case Terminated(ref) =>
        router = router.removeRoutee(ref)
        val newSlave = context.actorOf(Props[Slave])
        context.watch(newSlave)
        router = router.addRoutee(newSlave)
      // step 3 - route the messages
      case message =>
        router.route(message, sender())
    }
  }

  class Slave extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => log.info(message.toString)
    }
  }

  val system = ActorSystem("RouterDemo", ConfigFactory.load().getConfig("routersDemo"))
  val master = system.actorOf(Props[Master])


/*  for (i <- 1 to 10) {
    master ! s"[$i]Hello from the world"
  }*/

  /**
   * Method 2- a router actor with its own children
   * Pool router
   */
  // programmatically
  val poolMaster = system.actorOf(RoundRobinPool(5).props(Props[Slave]), "simplePoolMaster")
  for (i <- 1 to 10) {
    //poolMaster ! s"[$i] hello from the world"
  }

  // from configuration
  val poolMaster2 = system.actorOf(FromConfig.props(Props[Slave]), "poolMaster2")
  for (i <- 1 to 10) {
    // poolMaster2 ! s"[$i] hello from the world"
  }

  /**
   * Method 3 - router with actors created elsewhere
   * group router
   */
  val slaveList = (1 to 5).map(i => system.actorOf(Props[Slave], s"slave_$i")).toList
  val slavePaths = slaveList.map(slaveRef => slaveRef.path.toString)

  val groupMaster = system.actorOf(RoundRobinGroup(slavePaths).props())
  groupMaster ! Broadcast("hello, everyone")
  // PoisonPill and Kill are not routed
}
