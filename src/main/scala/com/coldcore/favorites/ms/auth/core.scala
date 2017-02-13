package com.coldcore.favorites
package ms.auth

import akka.actor.ActorSystem
import com.mongodb.casbah.MongoClient
import Constants._
import actors.TokenActor
import rest.AuthRestService
import db.MongoAuthDb

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}

object start {

  def createMongoClient(hostname: String, port: Int)(implicit system: ActorSystem, ec: ExecutionContext): MongoClient = {
    val mongoClient = MongoClient(hostname, port)
    system.whenTerminated.onComplete(_ => mongoClient.close())
    mongoClient
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
    val authDb = new MongoAuthDb(mongoClient, config.mongoDbName)

    val tokenActor = system.actorOf(TokenActor.props(authDb), name = s"$MS-token-actor")

    new AuthRestService(config.hostname, config.port, tokenActor)
  }
}

object Constants {
  val APP = "favorites"
  val MS = "ms-auth"
}
