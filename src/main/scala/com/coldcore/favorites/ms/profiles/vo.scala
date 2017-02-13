package com.coldcore.favorites
package ms.profiles.vo

import spray.json.DefaultJsonProtocol

case class Profile(profile_id: String, username: Option[String], email: Option[String])
object Profile extends DefaultJsonProtocol { implicit val format = jsonFormat3(apply) }

case class Token(username: String)
object Token extends DefaultJsonProtocol { implicit val format = jsonFormat1(apply) }
