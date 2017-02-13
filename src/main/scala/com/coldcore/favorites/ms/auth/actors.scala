package com.coldcore.favorites
package ms.auth.actors

import akka.actor.{Actor, ActorLogging, Props}
import ms.auth.db.AuthDb
import ms.auth.{BearerTokenGenerator, vo}

object TokenActor {
  def props(authDb: AuthDb): Props = Props(new TokenActor(authDb))

  case class LoginIN(username: String, password: String)
  case class LoginOUT(token: Option[vo.Token])

  case class ValidateTokenIN(token: String)
  case class ValidateTokenOUT(token: Option[vo.Token])
}

class TokenActor(authDb: AuthDb) extends Actor with ActorLogging {
  import TokenActor._

  val tgen = new BearerTokenGenerator

  def generateToken(username: String): vo.Token =
    vo.Token(tgen.generateSHAToken(username), "Bearer", 3600, username)

  def receive = {

    case LoginIN(username, password) =>
      val result =
        authDb.userLogin(username, password) match {
          case (true, t @ Some(_)) => t // already logged in
          case (true, None) => // create new token
            val newToken = generateToken(username)
            authDb.saveToken(newToken)
            Some(newToken)
          case _ => None // invalid username / password
        }

      sender ! LoginOUT(result)

    case ValidateTokenIN(token) =>
      sender ! ValidateTokenOUT(authDb.getToken(token))

  }

}
