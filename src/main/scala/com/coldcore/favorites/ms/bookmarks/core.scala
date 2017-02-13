package com.coldcore.favorites
package ms.bookmarks

import akka.actor.ActorSystem
import com.mongodb.casbah.MongoClient
import Constants._
import com.coldcore.favorites.ms.bookmarks.actors.BookmarksActor
import com.coldcore.favorites.ms.bookmarks.db.MongoBookmarksDb
import com.coldcore.favorites.ms.bookmarks.rest.BookmarksRestService
import ms.http.RestClient

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}

object start {

  def createMongoClient(hostname: String, port: Int)(implicit system: ActorSystem, ec: ExecutionContext): MongoClient = {
    val mongoClient = MongoClient(hostname, port)
    system.whenTerminated.onComplete(_ => mongoClient.close())
    mongoClient
  }

  def createRestClient()(implicit system: ActorSystem, ec: ExecutionContext): RestClient = {
    val restClient = new RestClient()
    system.whenTerminated.onComplete(_ => restClient.close())
    restClient
  }

  def main(args: Array[String]) = {
    implicit val system = ActorSystem(s"$APP-$MS")

    sys.addShutdownHook {
      println("Shutting down ...")
      system.terminate()
    }

    run

    Await.result(system.whenTerminated, Duration.Inf)
  }

  def run(implicit system: ActorSystem) {
    implicit val executionContext = system.dispatcher
    val config = Settings(system)

    val mongoClient = createMongoClient(config.mongoDbHostname, config.mongoDbPort)
    val bookmarksDb = new MongoBookmarksDb(mongoClient, config.mongoDbName)

    val restClient = createRestClient()

    val bookmarksActor = system.actorOf(BookmarksActor.props(bookmarksDb), name = s"$MS-actor")

    new BookmarksRestService(config.hostname, config.port, bookmarksActor, config.authBaseUrl, config.profilesBaseUrl, restClient)
  }
}

object Constants {
  val APP = "favorites"
  val MS = "ms-bookmarks"
}
