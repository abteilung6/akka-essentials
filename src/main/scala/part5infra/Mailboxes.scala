package org.abteilung6.akka
package part5infra

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.dispatch.{ControlMessage, PriorityGenerator, UnboundedPriorityMailbox}
import com.typesafe.config.{Config, ConfigFactory}

object Mailboxes extends App {
  val system = ActorSystem("MailboxDemo", ConfigFactory.load().getConfig("mailboxesDemo"))

  class SimpleActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => log.info(message.toString)
    }
  }

  /**
   * Interesting case #1 - custom priority mailbox
   * P0 -> most important
   * P1
   * P2
   * P3
   */

  // step 1 - mailbox defintion
/*  class SupportTicketPriorityMailbox(settings: ActorSystem.Settings, config: Config)
    extends UnboundedPriorityMailbox(PriorityGenerator{
      case message: String if message.startsWith("[P0]") => 0
      case message: String if message.startsWith("[P1]") => 1
      case message: String if message.startsWith("[P2]") => 2
      case message: String if message.startsWith("[P3]") => 3
    })

  // step 2 - make it known in the config
  // step 3 - attach the dispatcher to an actor
  val supportTicketLogger = system.actorOf(Props[SimpleActor].withDispatcher("support-ticket-dispatcher"))

  supportTicketLogger ! "[P3] this thing would be nice to have"
  supportTicketLogger ! "[P0] this needs to be solved NOW!"
  supportTicketLogger ! "[P1] do this when u have time"*/

  /**
   * Interesting case #2 - control-aware mailbox
   */
  // step 1 - mark import messages as control messages
  case object ManagementTicket extends ControlMessage

  // step 2 - configure who gets the mailbox
  val controlAwareActor = system.actorOf(Props[SimpleActor].withMailbox("control-mailbox"))
  controlAwareActor ! "[P3] this thing would be nice to have"
  controlAwareActor ! "[P0] this needs to be solved NOW!"
  controlAwareActor ! ManagementTicket

  // method 2 - using deployment config
}
