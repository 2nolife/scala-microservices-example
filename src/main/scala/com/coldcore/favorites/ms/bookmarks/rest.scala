package com.coldcore.favorites
package ms.bookmarks.rest

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
import ms.bookmarks.actors.BookmarksActor._
import ms.bookmarks.vo
import ms.http.RestClient

import scala.concurrent.duration._

class BookmarksRestService(hostname: String, port: Int, val bookmarksActor: ActorRef,
                          val authBaseUrl: String, val profilesBaseUrl: String,
                          val restClient: RestClient)(implicit system: ActorSystem)
  extends SprayJsonSupport with HeartbeatRoute with BookmarksRoute with OAuth2TokenValidator {

  val log = Logging.getLogger(system, this)

  implicit val executionContext = system.dispatcher
  implicit val materializer = ActorMaterializer()

  implicit val timeout = Timeout(5 seconds)

  val bindingFuture =
    Http().bindAndHandle(Route.handlerFlow(heartbeatRoute ~ bookmarksRoute), hostname, port)
  log.info(s"Bound Bookmarks REST to $hostname:$port")

  system.whenTerminated.onComplete { _ =>
    bindingFuture.flatMap(_.unbind())
  }

}

trait BookmarksRoute {
  self: BookmarksRestService =>

  def bookmarksRoute =
    validateProfile { profile =>
      path("bookmarks" ) {

        get {
          onSuccess(bookmarksActor ? GetBookmarksIN(profile)) {
            case GetBookmarksOUT(b) => complete { (StatusCodes.OK, b) }
          }
        } ~
        post {
          entity(as[vo.CreateBookmark]) { entity =>
            onSuccess(bookmarksActor ? CreateBookmarkIN(entity, profile)) {
              case CreateBookmarkOUT(Some(b)) => complete { (StatusCodes.Created, b) }
              case _ => complete { StatusCodes.Conflict }
            }
          }
        }

      } ~
      path("bookmarks" / Segment) { bookmarkId =>

        get {
          onSuccess(bookmarksActor ? GetBookmarkIN(bookmarkId, profile)) {
            case GetBookmarkOUT(Some(b)) => complete { (StatusCodes.OK, b) }
            case _ => complete { StatusCodes.NotFound }
          }
        } ~
        patch {
          entity(as[vo.UpdateBookmark]) { entity =>
            onSuccess(bookmarksActor ? UpdateBookmarkIN(bookmarkId, entity, profile)) {
              case UpdateBookmarkOUT(code, Some(b)) => complete { (code, b) }
              case UpdateBookmarkOUT(code, None) => complete { StatusCodes.getForKey(code).get }
            }
          }
        } ~
        delete {
          onSuccess(bookmarksActor ? DeleteBookmarkIN(bookmarkId, profile)) {
            case DeleteBookmarkOUT(code) => complete { StatusCodes.getForKey(code).get }
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
    val profilesBaseUrl: String
  } =>

  private def extractBearerToken(authHeader: Option[Authorization]): Option[String] =
    authHeader.collect { case Authorization(OAuth2BearerToken(token)) => token }

  private val authRejectedReason = AuthenticationFailedRejection(CredentialsRejected, HttpChallenges.oAuth2(""))

  def validateProfile: Directive1[vo.Profile] =
    extractRequestContext.flatMap { ctx =>
      extractBearerToken(ctx.request.header[Authorization]) match {
        case Some(access_token) =>

          val (code, profile) = restClient.restGet[vo.Profile](s"$profilesBaseUrl/profiles/me", access_token)
          profile.map(provide).getOrElse(reject(authRejectedReason))

        case _ =>
          reject(authRejectedReason)
      }
    }
}
