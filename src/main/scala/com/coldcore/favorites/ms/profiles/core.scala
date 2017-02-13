package com.coldcore.favorites
package ms.profiles

import akka.actor.ActorSystem
import com.mongodb.casbah.MongoClient
import Constants._
import ms.http.RestClient
import actors.ProfilesActor
import ms.profiles.rest.ProfilesRestService
import db.MongoProfilesDb

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
    val profilesDb = new MongoProfilesDb(mongoClient, config.mongoDbName)

    val restClient = createRestClient()

    val profilesActor = system.actorOf(ProfilesActor.props(profilesDb), name = s"$MS-actor")

    new ProfilesRestService(config.hostname, config.port, profilesActor, config.authBaseUrl, restClient)
  }
}

object Constants {
  val APP = "favorites"
  val MS = "ms-profiles"
}
