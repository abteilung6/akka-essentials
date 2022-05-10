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
}
