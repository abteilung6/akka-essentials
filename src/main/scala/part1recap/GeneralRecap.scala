package org.abteilung6.akka
package part1recap

import scala.util.Try

object GeneralRecap extends App {

  // value
  val aCondition: Boolean = false
  var aVariable: Int = 56
  aVariable += 1

  // expressions
  val anIfExpression = if (aCondition) 42 else 65

  // code block
  val aCodeBlock = {
    if (aCondition) 42 else 65
  }

  // Unit
  val theUnit: Unit = println("Hello Scala")

  def aFunction(x: Int) = x + 1

  // recursion - TAIL recursion
  def factorial(n: Int, acc: Int): Int =
    if (n <= 0) acc
    else factorial(n - 1, acc * n)

  // OOP
  class Animal

  class Dog extends Animal

  val aDog: Animal = new Dog

  trait Carnivore {
    def eat(a: Animal): Unit
  }

  class Crocodile extends Animal with Carnivore {
    override def eat(a: Animal): Unit = println("eat")
  }

  // method notaiton
  val aCroc = new Crocodile
  aCroc.eat(aDog)
  aCroc eat aDog

  // anonymous classes
  val aCarnivore = new Carnivore {
    override def eat(a: Animal): Unit = println("roar")
  }
  aCarnivore eat aDog

  // generics
  abstract class MyList[+A]

  // companion objects
  object MyList

  // case classes
  case class Person(name: String, age: Int)

  // exceptions
  val aPotentialFailure = try {
    throw new RuntimeException("")
  } catch {
    case e: Exception => "Caught exception"
  } finally {
    // side effects
    println("some logs")
  }

  // Functional programming
  val incrementer = new Function1[Int, Int] {
    override def apply(v1: Int): Int = v1 + 1
  }
  val incremented = incrementer(42) // 43
  // incrementer.apply(42)

  val anonymousIncrementer = (x: Int) => x + 1
  // Int => Int === Function1[Int, Int]

  // fp
  List(1, 2, 3).map(incrementer)

  val pairs = for {
    num <- List(1, 2, 3, 4)
    char <- List('a', 'b', 'c', 'd')
  } yield num + '_' + char

  // Seq, Array, List, Vector, Map, Tuples, Sets

  // "collections"
  // Options and Try
  val anOption = Some(2)
  val aTry = Try {
    throw new RuntimeException
  }

  // pattern matching
  val unknown = 2
  val order = unknown match {
    case 1 => "first"
    case 2 => "second"
    case _ => "unknown"
  }

  val bob = Person("bob", 22)
  val greeting = bob match {
    case Person(name, _) => s"name $name"
    case _ => "unknown"
  }
}
