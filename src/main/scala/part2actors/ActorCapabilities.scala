package org.abteilung6.akka
package part2actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

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

}
