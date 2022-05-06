package org.abteilung6.akka
package part2actors

import akka.actor.{Actor, ActorSystem, Props}

object ActorsIntro extends App {
  // part1 - actor systems
  val actorSystem = ActorSystem("firstActorSystem")
  println(actorSystem.name)

  // part2 - create actors
  // word count actor
  class WordCountActor extends Actor {
    var totalWords = 0

    // type Receive = PartialFunction[Any, Unit]
    override def receive: Receive = {
      case message: String =>
        println(s"[word counter] received ${message}")
        totalWords += message.split(" ").length
      case msg => println(s"[word counter] cannot understand ${msg.toString}")
    }
  }

  // part3 - instantiate actor
  val wordCounter = actorSystem.actorOf(Props[WordCountActor], "wordCounter")

  // part4 - communicate!
  wordCounter ! "sender a message with !"
  // wordCounter.!("sender a message with !")
  wordCounter ! "a different message"
  // asynchronous

  object Person {
    def props(name: String): Props = Props(new Person(name))
  }
  class Person(name: String) extends Actor {
    override def receive: Receive = {
      case "hi" => println(s"Hi my name is $name")
      case _ =>
    }
  }
  // params
  // val person = actorSystem.actorOf(Props(new Person("Bob")))
  val person = actorSystem.actorOf(Person.props("Bob")) // best practice
  person ! "hi"
}
