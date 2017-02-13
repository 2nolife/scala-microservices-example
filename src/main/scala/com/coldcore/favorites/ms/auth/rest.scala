package com.coldcore.favorites
package ms.auth.rest

import akka.actor.{ActorRef, ActorSystem}
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import ms.auth.actors.TokenActor._
import ms.auth.vo

import scala.concurrent.duration._

class AuthRestService(hostname: String, port: Int, val tokenActor: ActorRef)(implicit system: ActorSystem)
  extends SprayJsonSupport with HeartbeatRoute with TokenRoute {

  val log = Logging.getLogger(system, this)

  implicit val executionContext = system.dispatcher
  implicit val materializer = ActorMaterializer()

  implicit val timeout = Timeout(5 seconds)

  val bindingFuture =
    Http().bindAndHandle(Route.handlerFlow(heartbeatRoute ~ tokenRoute), hostname, port)
  log.info(s"Bound Auth REST to $hostname:$port")

  system.whenTerminated.onComplete { _ =>
    bindingFuture.flatMap(_.unbind())
  }

}

trait TokenRoute {
  self: AuthRestService =>

  def tokenRoute =
    path("auth" / "token") {

      post {
        entity(as[vo.Credentials]) { credentials =>
          onSuccess(tokenActor ? LoginIN(credentials.username, credentials.password)) {
            case LoginOUT(Some(t)) => complete { (StatusCodes.Created, t) }
            case _ => complete { StatusCodes.Unauthorized }
          }
        }
      } ~
      get {
        parameter('access_token) { token =>
          onSuccess(tokenActor ? ValidateTokenIN(token)) {
            case ValidateTokenOUT(Some(t)) => complete { t }
            case _ => complete { StatusCodes.Unauthorized }
          }
        }
      }

    }

}

trait HeartbeatRoute {

  def heartbeatRoute =
    path("heartbeat") {
      get {
        complete(HttpEntity(ContentTypes.`application/json`, """{"status": "ok"}"""))
      }
    }

}
