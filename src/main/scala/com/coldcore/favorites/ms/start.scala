package com.coldcore.favorites
package ms

import akka.actor.ActorSystem

import scala.concurrent.Await
import scala.concurrent.duration._

object start {

  def main(args: Array[String]) {
    implicit val system: ActorSystem = ActorSystem("slotsbooker")

    sys.addShutdownHook {
      println("Shutting down ...")
      system.terminate()
    }

    auth.start.run
    profiles.start.run
    bookmarks.start.run

    Await.result(system.whenTerminated, Duration.Inf)
  }

}
