package com.coldcore.favorites
package ms.profiles.rest

import akka.actor.{ActorRef, ActorSystem}
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.headers.{Authorization, HttpChallenges, OAuth2BearerToken}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.AuthenticationFailedRejection.CredentialsRejected
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import ms.http.RestClient
import ms.profiles.actors.ProfilesActor.{GetProfileByIdIN, GetProfileByIdOUT, GetProfileByUsernameIN, GetProfileByUsernameOUT}
import ms.profiles.vo

import scala.concurrent.duration._

class ProfilesRestService(hostname: String, port: Int,
                          val profilesActor: ActorRef, val authBaseUrl: String,
                          val restClient: RestClient)(implicit system: ActorSystem)
  extends SprayJsonSupport with HeartbeatRoute with ProfilesRoute with OAuth2TokenValidator {

  val log = Logging.getLogger(system, this)

  implicit val executionContext = system.dispatcher
  implicit val materializer = ActorMaterializer()

  implicit val timeout = Timeout(5 seconds)

  val bindingFuture =
    Http().bindAndHandle(Route.handlerFlow(heartbeatRoute ~ profilesRoute), hostname, port)
  log.info(s"Bound Profiles REST to $hostname:$port")

  system.whenTerminated.onComplete { _ =>
    bindingFuture.flatMap(_.unbind())
  }

}

trait ProfilesRoute {
  self: ProfilesRestService =>

  def profilesRoute =
    validateToken { token =>
      path("profiles" / "me") {

        get {
          onSuccess(profilesActor ? GetProfileByUsernameIN(token.username)) {
            case GetProfileByUsernameOUT(Some(p)) => complete { (StatusCodes.OK, p) }
            case _ => complete { StatusCodes.NotFound }
          }
        }

      } ~
      path("profiles" / Segment) { profileId =>

        get {
          onSuccess(profilesActor ? GetProfileByIdIN(profileId)) {
            case GetProfileByIdOUT(Some(p)) => complete { (StatusCodes.OK, p) }
            case _ => complete { StatusCodes.NotFound }
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

trait OAuth2TokenValidator {
  self: {
    val restClient: RestClient
    val authBaseUrl: String
  } =>

  private def extractBearerToken(authHeader: Option[Authorization]): Option[String] =
    authHeader.collect { case Authorization(OAuth2BearerToken(token)) => token }

  private val authRejectedReason = AuthenticationFailedRejection(CredentialsRejected, HttpChallenges.oAuth2(""))

  def validateToken: Directive1[vo.Token] =
    extractRequestContext.flatMap { ctx =>
      extractBearerToken(ctx.request.header[Authorization]) match {
        case Some(access_token) =>

          val (code, token) = restClient.restGet[vo.Token](s"$authBaseUrl/auth/token?access_token=$access_token")
          token.map(provide).getOrElse(reject(authRejectedReason))

        case _ =>
          reject(authRejectedReason)
      }
    }
}
