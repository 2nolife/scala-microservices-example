package com.coldcore.favorites
package ms.auth.db

import ms.auth.Constants._
import ms.auth.vo
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.MongoClient
import com.mongodb.casbah.commons.MongoDBObject

trait AuthDb extends {
  def userLogin(username: String, password: String): (Boolean, Option[vo.Token])
  def saveToken(token: vo.Token)
  def getToken(token: String): Option[vo.Token]
}

class MongoAuthDb(client: MongoClient, dbName: String) extends AuthDb {

  private val db = client(dbName)
  val users = db(s"$MS-users")
  val tokens = db(s"$MS-tokens")

  def asToken(data: MongoDBObject): vo.Token = {
    import data._
    val expires = (as[Number]("expires").longValue-System.currentTimeMillis)/1000L
    vo.Token(
      as[String]("token"),
      as[String]("type"),
      expires.toInt,
      as[String]("username"))
  }

  override def userLogin(username: String, password: String): (Boolean, Option[vo.Token]) =
    users
      .findOne(MongoDBObject("username" -> username, "password" -> password))
      .map { _ =>
        val existingToken =
          tokens
            .findOne(
              MongoDBObject(
                "username" -> username,
                "expires" -> MongoDBObject("$gt" -> System.currentTimeMillis)))
            .map(asToken(_))
        (true, existingToken)
      }
      .getOrElse(false, None)

  override def saveToken(token: vo.Token) {
    import token._
    val mills = expires*1000L+System.currentTimeMillis
    tokens.
      update(
        "username" $eq username,
        MongoDBObject("username" -> username, "token" -> access_token, "type" -> token_type, "expires" -> mills),
        upsert = true)
  }

  override def getToken(token: String): Option[vo.Token] =
    tokens
      .findOne(
        MongoDBObject(
          "token" -> token,
          "expires" -> MongoDBObject("$gt" -> System.currentTimeMillis)))
      .map(asToken(_))

}
