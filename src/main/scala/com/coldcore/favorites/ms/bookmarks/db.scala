package com.coldcore.favorites
package ms.bookmarks.db

import ms.db.MongoQueries
import ms.bookmarks.Constants._
import ms.bookmarks.vo
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.MongoClient
import com.mongodb.casbah.commons.MongoDBObject

trait BookmarksDb extends {
  def bookmarkById(bookmarkId: String): Option[vo.Bookmark]
  def createBookmark(profileId: String, obj: vo.CreateBookmark): vo.Bookmark
  def updateBookmark(bookmarkId: String, obj: vo.UpdateBookmark): Option[vo.Bookmark]
  def deleteBookmark(bookmarkId: String)
  def findBookmarks(profileId: String): Seq[vo.Bookmark]
}

class MongoBookmarksDb(client: MongoClient, dbName: String) extends BookmarksDb with MongoQueries {

  private val db = client(dbName)
  val bookmarks = db(MS)

  def asBookmark(data: MongoDBObject): vo.Bookmark = {
    import data._
    vo.Bookmark(
      bookmark_id = as[ObjectId]("_id").toString,
      profile_id = as[String]("profile_id"),
      url = as[String]("url"),
      rating = as[Int]("rating")
    )
  }

  override def bookmarkById(bookmarkId: String): Option[vo.Bookmark] =
    bookmarks
      .findOne(finderById(bookmarkId))
      .map(asBookmark(_))

  override def createBookmark(profileId: String, obj: vo.CreateBookmark): vo.Bookmark = {
    import obj._
    val bookmark = MongoDBObject(
      "profile_id" -> profileId,
      "url" -> url,
      "rating" -> rating)

    bookmarks.
      insert(bookmark)

    val bookmarkId = bookmark.idString
    bookmarkById(bookmarkId).get
  }

  override def updateBookmark(bookmarkId: String, obj: vo.UpdateBookmark): Option[vo.Bookmark] = {
    import obj._
    Map(
      "url" -> url,
      "rating" -> rating
    ).foreach { case (key, value) =>
      update(finderById(bookmarkId), bookmarks, key, value)
    }

    bookmarkById(bookmarkId)
  }

  override def deleteBookmark(bookmarkId: String) =
    bookmarks
      .findAndRemove(finderById(bookmarkId))

  override def findBookmarks(profileId: String): Seq[vo.Bookmark] = {
    bookmarks
      .find("profile_id" $eq profileId)
      .map(asBookmark(_))
      .toSeq
  }

}
