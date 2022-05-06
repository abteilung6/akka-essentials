package org.abteilung6.akka
package part1recap

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

object MultithreadingRecap extends App {

  // creating threads on the JVM

  val aThreadOld = new Thread(new Runnable {
    override def run(): Unit = println("running parallel")
  })

  val aThread = new Thread(() => println("running parallel"))
  aThread.start()
  aThread.join() // wait until finished

  // different runs produce different results
  val threadHello = new Thread(()=> (1 to 1000).foreach(_ => println("hello")))
  val threadGoodbye = new Thread(()=> (1 to 1000).foreach(_ => println("goodbye")))
  threadHello.start()
  threadGoodbye.start()

  // @volatile locks amount for read and write
  class BankAccount(@volatile private var amount: Int) {
    override def toString: String = "" + amount

    // not atomic
    def withdraw(money: Int): Unit = this.amount -= money

    def safeWithdraw(money: Int): Unit = this.synchronized {
      this.amount -= money
    }
  }

  // inter-thread communication on the JVM
  // wait - notify mechanism

  // Scala futures
  import scala.concurrent.ExecutionContext.Implicits.global
  val future = Future {
    // long computation - on a different thread
    42
  }

  // callbacks
  future.onComplete {
    case Success(42) => print("found")
    case Failure(_) => println("failure happened")
  }

  val aProcessedFuture = future.map(_ + 1) // Future with 43
  val aFlatFuture = future.flatMap { value =>
    Future(value + 2)
  }
  val filteredFuture = future.filter(_ % 2 == 0)

  // for comprehensions
  val aNonsenseFuture = for {
    meaningOfLife <- future
    filteredMeaning <- filteredFuture
  } yield meaningOfLife + filteredMeaning

  // andThen, recover/recoverWith

  // Promises
}
