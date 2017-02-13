package com.coldcore.favorites
package ms.bookmarks.actors

import akka.actor.{Actor, ActorLogging, Props}
import ms.bookmarks.db.BookmarksDb
import ms.bookmarks.vo
import org.apache.http.HttpStatus._

object BookmarksActor {
  def props(bookmarksDb: BookmarksDb): Props = Props(new BookmarksActor(bookmarksDb))

  case class GetBookmarkIN(bookmarkId: String, profile: vo.Profile)
  case class GetBookmarkOUT(bookmark: Option[vo.Bookmark])

  case class GetBookmarksIN(profile: vo.Profile)
  case class GetBookmarksOUT(bookmark: Seq[vo.Bookmark])

  case class CreateBookmarkIN(obj: vo.CreateBookmark, profile: vo.Profile)
  case class CreateBookmarkOUT(bookmark: Option[vo.Bookmark])

  case class UpdateBookmarkIN(bookmarkId: String, obj: vo.UpdateBookmark, profile: vo.Profile)
  case class UpdateBookmarkOUT(code: Int, bookmark: Option[vo.Bookmark])

  case class DeleteBookmarkIN(bookmarkId: String, profile: vo.Profile)
  case class DeleteBookmarkOUT(code: Int)
}

class BookmarksActor(val bookmarksDb: BookmarksDb) extends Actor with ActorLogging {
  import BookmarksActor._

  def receive = {

    case GetBookmarkIN(bookmarkId, profile) =>
      val bookmark = bookmarksDb.bookmarkById(bookmarkId)

      sender ! GetBookmarkOUT(bookmark)

    case GetBookmarksIN(profile) =>
      val bookmarks = bookmarksDb.findBookmarks(profile.profile_id)

      sender ! GetBookmarksOUT(bookmarks)

    case CreateBookmarkIN(obj, profile) =>
      val found =
        bookmarksDb
          .findBookmarks(profile.profile_id)
          .exists(_.url == obj.url)

      val bookmark =
        if (!found) Some(bookmarksDb.createBookmark(profile.profile_id, obj)) else None

      sender ! CreateBookmarkOUT(bookmark)

    case UpdateBookmarkIN(bookmarkId, obj, profile) =>
      val myBookmark = bookmarksDb.bookmarkById(bookmarkId)

      val (code, bookmark) =
        if (myBookmark.isEmpty) (SC_NOT_FOUND, None)
        else if (myBookmark.get.profile_id != profile.profile_id) (SC_FORBIDDEN, None)
        else {
          val bookmark = bookmarksDb.updateBookmark(bookmarkId, obj)
          (SC_OK, bookmark)
        }

      sender ! UpdateBookmarkOUT(code, bookmark)

    case DeleteBookmarkIN(bookmarkId, profile) =>
      val myBookmark = bookmarksDb.bookmarkById(bookmarkId)

      val code =
        if (myBookmark.isEmpty) SC_NOT_FOUND
        else if (myBookmark.get.profile_id != profile.profile_id) SC_FORBIDDEN
        else {
          bookmarksDb.deleteBookmark(bookmarkId)
          SC_OK
        }

      sender ! DeleteBookmarkOUT(code)

  }

}
