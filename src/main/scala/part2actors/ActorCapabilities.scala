package org.abteilung6.akka
package part2actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import org.abteilung6.akka.part2actors.ActorCapabilities.BankAccount.{Deposit, Statement, WithDraw}

object ActorCapabilities extends App {

  class SimpleActor extends Actor {
    // context.self reference to actor object === self

    override def receive: Receive = {
      case "Hi!" => context.sender() ! "Hello there!" // replies to a message
      case message: String => println(s"[${context.self.path}] received message $message")
      case number: Int => println(s"[${self.path}] received a number: $number")
      case SpecialMessage(contents) => println(s"[simple actor] received special $contents")
      case SendMessageToYourself(content) =>
        self ! content // sends again and will be matched with `case message`
      case SayHiTo(ref) => ref ! "Hi!" // ! implicit (ref) lese no sender

      case WirelessPhoneMessage(content, ref) => ref forward (content + "s") // keep the original sender
    }
  }

  val system = ActorSystem("actorCapabilitiesDemo")
  val simpleActor = system.actorOf(Props[SimpleActor], "simpleActor")

  simpleActor ! "hello actor"

  // 1 - messages can be of any type
  // a) message be immutable
  // b) message must be serializable (transformed to byte stream)
  // in practice use case classes and case objects
  simpleActor ! 42 // received number 42

  case class SpecialMessage(contents: String)
  simpleActor ! SpecialMessage("some special content")

  // 2 - actors have information
  // context.self === this in OOP

  case class SendMessageToYourself(content: String)
  simpleActor ! SendMessageToYourself("I am an actor and i am proud of it")

  // 3- actors can reply to messages
  val alice = system.actorOf(Props[SimpleActor], "alice")
  val bob = system.actorOf(Props[SimpleActor], "bob")

  case class SayHiTo(ref: ActorRef)
  alice ! SayHiTo(bob) //  alice sends message to bob by reference, bob replies

  alice ! "Hi!" // implicit, replies to "me" -> dead letter (no sender)

  // 5 - forwarding messages
  // D -> A -> B
  // forwarding = sending a message with the original sender
  case class WirelessPhoneMessage(content: String, ref: ActorRef)
  alice ! WirelessPhoneMessage("Maga", bob) // alice forwards to bob

  /**
   * Exercises
   * 1.a Counter actor
   *  - increment
   *  - decrement
   *  - print
   *
   * 2. a Bank account as an actor
   *  receives
   *  - deposit an amount
   *  - withdraw an amount
   *  - statement
   *  replies
   *  - Success Failure
   *
   *  interact with some other kind of actor
   */

  // Domain of the counter
  object Counter {
    case object Increment
    case object Decrement
    case object Print
  }


  class Counter extends Actor {
    import Counter._
    var count = 0

    override def receive: Receive = {
      case Increment => count += 1
      case Decrement => count -= 1
      case Print => println(s"[counter] My current count is $count")
    }
  }

  import Counter._
  val counter = system.actorOf(Props[Counter], "myCounter")
  (1 to 5).foreach(_ => counter ! Increment)
  (1 to 3).foreach(_ => counter ! Decrement)
  counter ! Print

  object BankAccount {
    case class Deposit(amount: Int)
    case class WithDraw(amount: Int)
    case object Statement

    case class TransactionSuccess(message: String)
    case class TransactionFailure(reason: String)
  }
  // bank account
  class BankAccount extends Actor {
    import BankAccount._
    var funds = 0

    override def receive: Receive = {
      case Deposit(amount) =>
        if (amount < 0) sender() ! TransactionFailure("Invalid deposit amount")
        else {
          funds += amount
          sender() ! TransactionSuccess(s"successfully deposited $amount")
        }
      case WithDraw(amount) =>
        if (amount < 0) sender() ! TransactionFailure("invalid withdraw amount")
        else if (amount > funds) sender() ! TransactionFailure("Insufficient funds")
        else {
          funds -= amount
          sender() ! TransactionSuccess(s"successfully withdraw $amount")
        }
      case Statement => sender() ! s"Your balance is $funds"
    }
  }

  object Person {
    case class LiveTheLife(account: ActorRef)
  }
  class Person extends Actor {
    import Person._
    override def receive: Receive = {
      case LiveTheLife(account) =>
        account ! Deposit(10000)
        account ! WithDraw(90000)
        account ! WithDraw(500)
        account ! Statement
      case message => println(message.toString)
    }
  }

  val account = system.actorOf(Props[BankAccount], "bankAccount")
  val person = system.actorOf(Props[Person], "billionaire")

  import Person._
  person ! LiveTheLife(account)
}
