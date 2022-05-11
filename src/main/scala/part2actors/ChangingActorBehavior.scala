package org.abteilung6.akka
package part2actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import org.abteilung6.akka.part2actors.ChangingActorBehavior.Mom.MomStart

object ChangingActorBehavior extends App {

  object FussyKid {
    case object KidAccept
    case object KidReject
    val HAPPY = "happy"
    val SAD = "sad"
  }
  class FussyKid extends Actor {
    import FussyKid._
    import Mom._
    var state: String = HAPPY
    override def receive: Receive = {
      case Food(VEGETABLE) => state = SAD
      case Food(CHOCOLATE) => state = HAPPY
      case Ask(_) =>
        if (state == HAPPY) sender() ! KidAccept
        else sender() ! KidReject
    }
  }

  class StatelessFussyKid extends Actor {
    import FussyKid._
    import Mom._

    override def receive: Receive = happyReceive

    def happyReceive: Receive = {
      case Food(VEGETABLE) => // change my receive handler to sad Receive
        context.become(sadReceive, discardOld = false)
      case Food(CHOCOLATE) => //
      case Ask(_) => sender() ! KidAccept
    }
    def sadReceive: Receive = {
      case Food(VEGETABLE) => context.become(sadReceive, discardOld = false)
      case Food(CHOCOLATE) =>  context.unbecome()
      case Ask(_) => sender() ! KidReject
    }
  }

  object Mom {
    case class MomStart(kidRef: ActorRef)
    case class Food(food: String)
    case class Ask(message: String) // do you want to play
    val VEGETABLE = "veggies"
    val CHOCOLATE = "chocolate"

  }
  class Mom extends Actor {
    import Mom._
    import FussyKid._
    override def receive: Receive = {
      case MomStart(kidRef) =>
        kidRef ! Food(VEGETABLE)
        kidRef ! Ask("do you want to play")
        kidRef ! Food(CHOCOLATE)
        kidRef ! Ask("do you want to play")
        kidRef ! Food(VEGETABLE)
        kidRef ! Food(VEGETABLE)
        kidRef ! Food(CHOCOLATE)
        kidRef ! Ask("do you want to play")
      case KidAccept => println("my kid is happy")
      case KidReject => println("my kids is sad")
    }
  }

  val system = ActorSystem("changingActorBehaviour")
  val kid = system.actorOf(Props[FussyKid], "fuzzyKid")
  val mom = system.actorOf(Props[Mom], "mom")
  val statelessFussyKid = system.actorOf(Props[StatelessFussyKid])

  // mom ! MomStart(kid)
  mom ! MomStart(statelessFussyKid)

  /**
   * move receives MomStart
   * kid receives Food(veg) => kid will change the handler to sadReceive
   * kid receives Ask(play?) => kid replies with the sad receive handler
   *
   */

  /**
   * Food(veg) => stask.push(sadReceive)
   * Food(choc) => stack.push(happyReceive)
   *
   * Stack:
   * 1. happyReceive <- akka calls always the first in the cack
   * 2. sadReceive
   * 3. happyReceive
   */

  /**
   * Exercise
   * 1 - recreate the Counter Actor with context become and no mutable state
   * 2 - simplified voting system
   */

  object Counter {
    case object Increment
    case object Decrement
    case object Print
  }


  class Counter extends Actor {
    import Counter._

    override def receive: Receive = countReceive(0)

    def countReceive(currentCount: Int): Receive = {
      case Increment =>
        println(s"[countReceive($currentCount)] incrementing")
        context.become(countReceive(currentCount + 1))
      case Decrement =>
        println(s"[countReceive($currentCount)] decrementing")
        context.become(countReceive((currentCount - 1)))
      case Print => println(s"[counterReceive($currentCount)] my current count is $currentCount")
    }
  }

  import Counter._
  val counter = system.actorOf(Props[Counter], "myCounter")
  (1 to 5).foreach(_ => counter ! Increment)
  (1 to 3).foreach(_ => counter ! Decrement)
  counter ! Print

  /**
   * Exercise 2 - a simplified voting system
   */

  case class Vote(candidate: String)
  case class VoteStatusRequest()
  case class VoteStatusReply(candidate: Option[String])
  class Citizen extends Actor {
    override def receive: Receive = {
      case Vote(c) => context.become(voted(c))
        //this.candidate = Some(c)
      case VoteStatusRequest => sender() ! VoteStatusReply(None)
    }

    def voted(candidate: String): Receive = {
      case VoteStatusRequest => sender() ! VoteStatusReply(Some(candidate))
    }
  }

  case class AggregatedVotes(citizens: Set[ActorRef])
  class VoteAggregator extends Actor {
    // var stillWaiting: Set[ActorRef] = Set()
    // var currentStats: Map[String, Int] = Map()

    override def receive: Receive = awaitingCommand

    def awaitingCommand: Receive = {
      case AggregatedVotes(citizens) =>
        citizens.foreach(citizenRef => citizenRef ! VoteStatusRequest)
        context.become(awaitingStatuses(citizens, Map()))
    }

    def awaitingStatuses(stillWaiting: Set[ActorRef], currentStats: Map[String, Int]): Receive = {
      case VoteStatusReply(None) =>
        // a citizen hasn't voted yet
        sender() ! VoteStatusRequest // might end up in an infinite loop
      case VoteStatusReply(Some(candidate)) =>
        val newStillWaiting = stillWaiting - sender()
        val currentVotesOfCandidate = currentStats.getOrElse(candidate, 0)
        val newStats = currentStats + (candidate -> (currentVotesOfCandidate + 1))
        if (newStillWaiting.isEmpty) {
          println(s"[aggregator] poll stats: $newStats")
        } else {
          // need to process some statuses
          context.become(awaitingStatuses(newStillWaiting, newStats))
        }
    }
  }

  val alice = system.actorOf(Props[Citizen])
  val bob = system.actorOf(Props[Citizen])
  val charlie = system.actorOf(Props[Citizen])
  val daniel = system.actorOf(Props[Citizen])

  alice ! Vote("Martin")
  bob ! Vote("Jonas")
  charlie ! Vote("Roland")
  daniel ! Vote("Roland")

  val voteAggregator = system.actorOf(Props[VoteAggregator])
  voteAggregator ! AggregatedVotes(Set(alice, bob, charlie, daniel))

  /**
   * Print the status fo the votes
   * Martin -> 1
   * Jonas -> 1
   * Roland -> 2
   */

}
