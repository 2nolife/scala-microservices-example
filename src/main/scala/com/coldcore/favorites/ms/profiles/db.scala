package com.coldcore.favorites
package ms.profiles.db

import ms.db.MongoQueries
import ms.profiles.Constants._
import ms.profiles.vo
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.MongoClient
import com.mongodb.casbah.commons.MongoDBObject

trait ProfilesDb extends {
  def profileByUsername(username: String): Option[vo.Profile]
  def profileById(profileId: String): Option[vo.Profile]
}

class MongoProfilesDb(client: MongoClient, dbName: String) extends ProfilesDb with MongoQueries {

  private val db = client(dbName)
  val profiles = db(MS)

  def asProfile(data: MongoDBObject): vo.Profile = {
    import data._
    vo.Profile(
      profile_id = as[ObjectId]("_id").toString,
      username = getAs[String]("username"),
      email = getAs[String]("email")
    )
  }

  override def profileByUsername(username: String): Option[vo.Profile] =
    profiles
      .findOne("username" $eq username)
      .map(asProfile(_))

  override def profileById(profileId: String): Option[vo.Profile] =
    profiles
      .findOne(finderById(profileId))
      .map(asProfile(_))

}
