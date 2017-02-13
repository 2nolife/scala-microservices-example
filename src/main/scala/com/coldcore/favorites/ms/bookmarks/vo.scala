package com.coldcore.favorites
package ms.bookmarks.vo

import spray.json.DefaultJsonProtocol

case class Profile(profile_id: String)
object Profile extends DefaultJsonProtocol { implicit val format = jsonFormat1(apply) }

case class Token(username: String)
object Token extends DefaultJsonProtocol { implicit val format = jsonFormat1(apply) }

case class Bookmark(bookmark_id: String, profile_id: String, url: String, rating: Int)
object Bookmark extends DefaultJsonProtocol { implicit val format = jsonFormat4(apply) }

case class CreateBookmark(url: String, rating: Int)
object CreateBookmark extends DefaultJsonProtocol { implicit val format = jsonFormat2(apply) }

case class UpdateBookmark(url: Option[String], rating: Option[Int])
object UpdateBookmark extends DefaultJsonProtocol { implicit val format = jsonFormat2(apply) }
