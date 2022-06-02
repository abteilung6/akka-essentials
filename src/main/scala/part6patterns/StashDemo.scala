package org.abteilung6.akka
package part6patterns

import akka.actor.{Actor, ActorLogging, ActorSystem, Props, Stash}

object StashDemo extends App {

  /**
   * ResourceActor
   * - open => can receive read/write requests to the resource
   * - otherwise it will postpone all read/write requests until the state is open
   *
   * ResourceActor is closed
   * - Open => switch to the open state
   *
   * ResourceActor is open
   * - Read, Write are handled
   * - Close => switch to the closed state
   *
   * [Open, Read, Read, Write]
   * - switch to the open state
   * - 2 x read the data
   * - write the data
   *
   * [Read, Open, Write]
   * - stash Read => Stash: [Read]
   * - switch to the open state
   * - read and write
   */
  case object Open
  case object Close
  case object Read
  case class Write(data: String)

  class ResourceActor extends Actor with ActorLogging with Stash {
    private var innerData: String = ""

    override def receive: Receive = closed

    def closed: Receive = {
      case Open =>
        log.info("Opening resource")
        unstashAll()
        context.become(open)
      case message =>
        log.info(s"Stashing $message in closed state")
        stash()
    }

    def open: Receive = {
      case Read =>
        log.info(s"Read $innerData")
      case Write(data) =>
        log.info(s"Write $data")
        innerData = data
      case Close =>
        log.info("Closing resource")
        unstashAll()
        context.become(closed)
      case message =>
        log.info(s"Stashing $message in open state")
        stash()
    }
  }

  val system = ActorSystem("StashDemo")
  val resourceActor = system.actorOf(Props[ResourceActor])

  resourceActor ! Read // stashed
  resourceActor ! Open // switch to open
  resourceActor ! Open // stashed
  resourceActor ! Write("i love stash") // write ...
  resourceActor ! Close // switch to close
  resourceActor ! Read // stashed
}
