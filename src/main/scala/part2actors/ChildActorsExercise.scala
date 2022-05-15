package org.abteilung6.akka
package part2actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object ChildActorsExercise extends App {
  // Distributed Word counting

  object WordCountMaster {
    case class Initialize(nChildren: Int)

    case class WordCountTask(id: Int, text: String)

    case class WordCountReply(id: Int, count: Int)
  }

  class WordCountMaster extends Actor {

    import WordCountMaster._

    override def receive: Receive = {
      case Initialize(nChildren) =>
        println("[master] initializing...")
        val childrenRefs = for (i <- 1 to nChildren) yield context.actorOf(Props[WordCountWorker], s"wcw_$i")
        context.become(withChildren(childrenRefs, 0, 0 , Map()))
    }

    def withChildren(childrenRefs: Seq[ActorRef], currentChildIndex: Int, currentTaskId: Int, requestMap: Map[Int, ActorRef]): Receive = {
      case text: String =>
        println(s"[master] received: $text -  will send to child $currentChildIndex")
        val originalSender = sender()
        val task = WordCountTask(currentTaskId, text)
        val childRef = childrenRefs(currentChildIndex)
        childRef ! task
        val nextChildIndex = (currentChildIndex + 1) % childrenRefs.length
        val nextTaskId = currentTaskId + 1
        val nextRequestMap = requestMap + (currentTaskId -> originalSender)
        context.become(withChildren(childrenRefs, nextChildIndex, nextTaskId, nextRequestMap))
      case WordCountReply(id, count) =>
        println(s"[master] received reply for task $id with count $count")
        val originalSender = requestMap(id)
        originalSender ! count
        context.become(withChildren(childrenRefs, currentChildIndex, currentTaskId, requestMap - id))
    }
  }

  class WordCountWorker extends Actor {

    import WordCountMaster._

    override def receive: Receive = {
      case WordCountTask(id, text) =>
        println(s"${self.path} received task $id with $text")
        sender() ! WordCountReply(id, text.split(" ").length)
    }
  }

  class TestActor extends Actor {
    import WordCountMaster._

    override def receive: Receive = {
      case "go" =>
        val master = context.actorOf(Props[WordCountMaster], "master")
        master ! Initialize(3)
        val texts = List("I love akka", "scala is super", "yes", "me too")
        texts.foreach(text => master ! text)
      case count: Int =>
        println(s"[test actor] received reply $count")
    }
  }

  val system = ActorSystem("roundRobinWordCountExercise")
  val testActor = system.actorOf(Props[TestActor], "testActor")
  testActor ! "go"

  /**
   * create WordCountMaster
   * send Initialize(10) to wordCountMaster // creates 10 WordCountWorkers
   * sender "Akka is awesome" to wordCounterMaster
   * - will send a WordCountTask("...") to one of its children
   * - child replies with WordCountReply(3) to the master
   * master replies with 3 to the sender
   *
   * requester -> wcm -> wcw
   * wcw -> wcm -> requester
   */

  // round robin logic

}
