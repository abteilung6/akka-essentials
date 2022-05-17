package org.abteilung6.akka
package part2actors

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import com.typesafe.config.ConfigFactory

object IntroAkkaConf extends App {

  class SimpleLoggingActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => log.info(message.toString)
    }
  }

  val configString =
    """
      | akka {
      |   loglevel = "INFO"
      | }
      |""".stripMargin

  val config = ConfigFactory.parseString(configString)
  val system = ActorSystem("actorSystem", ConfigFactory.load(config))
  val actor = system.actorOf(Props[SimpleLoggingActor])
  actor ! "foo"

  // loads from resources/application.conf
  val defaultConfigActorSystem = ActorSystem("defaultActorSystem")
  val defaultConfigActor = defaultConfigActorSystem.actorOf(Props[SimpleLoggingActor])
  defaultConfigActor ! "remember me"

  // separate namespace in config
  val specialConfig = ConfigFactory.load().getConfig("mySpecialConfig")
  val specialConfigSystem = ActorSystem("specialConfigSystem", specialConfig)
  val specialActor = specialConfigSystem.actorOf(Props[SimpleLoggingActor], "specialActor")
  specialActor ! "special actor"

  // separate configuration
  val separateConfig = ConfigFactory.load("secretFolder/secretConfiguration.conf")
  println(separateConfig.getString("akka.loglevel"))

  // json and properties possible..
}
