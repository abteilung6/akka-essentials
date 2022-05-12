package org.abteilung6.akka
package part2actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import org.abteilung6.akka.part2actors.ChildActors.CreditCard.{AttachToAccount, CheckStatus}

object ChildActors extends App {

  // Actors can create other actors

  object Parent {
    case class CreateChild(name: String)
    case class TellChild(message: String)
  }
  class Parent extends Actor {
    import  Parent._

    override def receive: Receive = {
      case CreateChild(name: String) =>
        println(s"${self.path} creating child")
        // create a new actor right here
        val childRef = context.actorOf(Props[Child], name)
        context.become(withChild(childRef))
    }

    def withChild(childRef: ActorRef): Receive = {
      case TellChild(message) => childRef forward message
    }
  }

  class Child extends Actor {
    override def receive: Receive = {
      case message => println(s"${self.path} got: $message")
    }
  }

  import Parent._
  val actorSystem = ActorSystem("ParentChildDemo")
  val parent = actorSystem.actorOf(Props[Parent], "parent")
  parent ! CreateChild("child")
  parent ! TellChild("hey kid")

  // actor hierarchies
  // parent -> child -> grandChild
  //        -> child 2 ->

  /**
   * Guardian actors (top level)
   * - /system = system guardian
   * - /user = user-level guardian
   * - / = the root guardian (manages system and user guardian)
   */

  /**
   * Actor selection
   */
  val childSelection = actorSystem.actorSelection("/user/parent/child") // if not found -> EmptyLocalActorRef
  childSelection ! "I found you"

  /**
   * DANGER !!
   * Never pass mutable actor state or this reference to child actors.
   */

  object NaiveBankAccount {
    case class Deposit(amount: Int)
    case class WithDraw(amount: Int)
    case object InitializeAccount
  }
  class NaiveBankAccount extends Actor {
    import NaiveBankAccount._
    import CreditCard._
    var amount = 0
    override def receive: Receive = {
      case InitializeAccount =>
        val creditCardRef = context.actorOf(Props[CreditCard], "card")
        creditCardRef ! AttachToAccount(this) // !!
      case Deposit(funds) => deposit(funds)
      case WithDraw(funds) => withDraw(funds)
    }

    def deposit(funds: Int): Unit = {
      println(s"${self.path} depositing $funds on top of $amount")
      amount += funds
    }
    def withDraw(funds: Int): Unit = {
      println(s"${self.path} withdrawing $funds from $amount")
      amount -= funds
    }
  }

  object CreditCard {
    case class AttachToAccount(bankAccount: NaiveBankAccount) // !! actorRef expected
    case object CheckStatus
  }
  class CreditCard extends Actor {
    override def receive: Receive = {
      case AttachToAccount(account) => context.become(attachTo(account))
    }

    def attachTo(account: NaiveBankAccount): Receive = {
      case CheckStatus =>
        println(s"${self.path} your message has been processed.")
        account.withDraw(1)
    }
  }

  import NaiveBankAccount._
  import CreditCard._

  val bankAccountRef = actorSystem.actorOf(Props[NaiveBankAccount], "account")
  bankAccountRef ! InitializeAccount
  bankAccountRef ! Deposit(100)

  Thread.sleep(500)
  val ccSelection = actorSystem.actorSelection("/user/account/card")
  ccSelection ! CheckStatus //

  // Wrong !! called closing over
}
