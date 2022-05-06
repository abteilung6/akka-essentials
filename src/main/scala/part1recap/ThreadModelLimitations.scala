package org.abteilung6.akka
package part1recap

import scala.concurrent.Future

object ThreadModelLimitations extends App {

  /*
  Daniel's rants
   */

  // DR #1: OO encapsulation is only valid in the SINGLE-THREADED MODEL
  class BankAccount(private var amount: Int) {
    override def toString: String = "" + amount
    def withdraw(money: Int): Unit = this.amount -= money
    def deposit(money: Int): Unit = this.amount += money
    def getAmount: Int = amount
  }

/*  val account = new BankAccount(2000)
  for(_ <- 1 to 1000) {
    new Thread(() => account.withdraw(1)).start()
  }

  for(_ <- 1 to 1000) {
    new Thread(() => account.deposit(1)).start()
  }*/
  //println(account.getAmount) // 1999, not 2000 (not synchronized)
  // deadlocks, livelocks

  // Need a data structure, fully encapsulated, with no locks

  // DR #2 - delegating a task to a thread
  // you have a running thread and you want a to pass a runnable t that thread.

  var task: Runnable = null

  val runningThread: Thread = new Thread(() => {
    while (true) {
      while (task == null) {
        runningThread.synchronized {
          println("waiting for a task...")
          runningThread.wait()
        }
      }

      task.synchronized {
        println("i have a task")
        task.run()
        task = null
      }
    }
  })

  def delegateToBackgroundThread(r: Runnable): Unit = {
    if (task == null) task = r

    runningThread.synchronized {
      runningThread.notify()
    }
  }

  runningThread.start()
  Thread.sleep(500)
  delegateToBackgroundThread(() => println(42))
  Thread.sleep(1000)
  delegateToBackgroundThread(() => println("this should run in the background"))

  // Need a data structure, safely receive message, identify sender, ..

  /**
   * D3 #3: tracing and dealing with errors in a multithreaded env
   */
  // 1M numbers n between 10 threads
  import scala.concurrent.ExecutionContext.Implicits.global

  val futures = (0 to 9)
    .map(i => 100_000 * i until 100_000 * (i + 1)) // 0 - 99999, 100_000 - 199999, 200_000 - 299999
    .map(range => Future {
      if (range.contains(546735)) throw new RuntimeException("invalid number")
      range.sum
    })

  val sumFuture = Future.reduceLeft(futures)(_ + _) // Future with the sum of all the numbers
  sumFuture.onComplete(println)

}

