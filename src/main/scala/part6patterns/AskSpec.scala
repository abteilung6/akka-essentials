package org.abteilung6.akka
package part6patterns

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import scala.util.{Failure, Success}


class AskSpec extends TestKit(ActorSystem("AskSpec"))
 with ImplicitSender
 with AnyWordSpecLike
 with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  import AskSpec._

  "An authenticator" should {
    import AuthManager._
    "fail to authenticate a non-registered user" in {
      val authManager = system.actorOf(Props[AuthManager])
      authManager ! Authenticate("foo", "bar")
      expectMsg(AuthFailure(AUTH_FAILURE_NOT_FOUND))
    }
  }

}

object AskSpec {

  case class Read(key: String)
  case class Write(key: String, value: String)
  class KVActor extends Actor with ActorLogging {
    override def receive: Receive = online(Map())

    def online(kv: Map[String, String]): Receive = {
      case Read(key) =>
        log.info(s"Trying to read the value at the key $key")
        sender() ! kv.get(key)
      case Write(key, value) =>
        log.info(s"Writing the value $value for the key $key")
        context.become(online(kv + (key -> value)))
    }
  }

  case class RegisterUser(username: String, password: String)
  case class Authenticate(username: String, password: String)
  case class AuthFailure(message: String)
  case object AuthSuccess
  object AuthManager {
    val AUTH_FAILURE_NOT_FOUND = "username not found"
    val AUTH_FAILURE_PASSWORD_INCORRECT = "password incorrect"
    val AUTH_FAILURE_SYSTEM = "system error"
  }

  class AuthManager extends Actor with ActorLogging {
    import AuthManager._

    // step 2 - logistics
    implicit val timeout: Timeout = Timeout(1 second)
    implicit val executionContext: ExecutionContext = context.dispatcher

    private val authDb = context.actorOf(Props[KVActor])

    override def receive: Receive = {
      case RegisterUser(username, password) => authDb ! Write(username, password)
      case Authenticate(username, password) =>
        val originalSender = sender()
        // step 3 - ask the actor
        val future = authDb ? Read(username)
        // step 4 - handle the future
        future.onComplete {
          // step 5 - most important,
          // never call methods on the actor instance or
          // access mutable state in oncomplete
          case Success(None) => sender() ! AuthFailure(AUTH_FAILURE_NOT_FOUND)
          case Success(Some(dbPassword)) =>
            if(dbPassword == password) originalSender ! AuthSuccess
            else originalSender ! AuthFailure(AUTH_FAILURE_PASSWORD_INCORRECT)
          case Failure(_) => originalSender ! AuthFailure(AUTH_FAILURE_SYSTEM)
        }
    }
  }
}
