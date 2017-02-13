package com.coldcore.favorites
package test

import akka.http.scaladsl.model.headers.Authorization
import org.apache.http.HttpStatus._
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FlatSpec, Matchers}
import ms.bookmarks.vo

class MsBookmarksSpec extends FlatSpec with BeforeAndAfterAll with BeforeAndAfter with Matchers with HelperObjects {

  override protected def beforeAll() {
    systemStart()

    mongoDropDatabase()

    mongoSetupTestUser()
    mongoSetupUser("testuser2")
  }

  override protected def afterAll() {
    systemStop()
  }

  before {
    mongoRemoveBookmarks()
    mongoRemoveBookmarks("testuser2")
  }

  "GET /bookmarks/{id}" should "give 401 with invalid bearer token" in {
    val url = s"$bookmarksBaseUrl/bookmarks/$randomId"
    val headers = Seq((Authorization.name, s"Bearer ABCDEF"))
    When getTo url withHeaders headers expect() code SC_UNAUTHORIZED
  }

  "GET /bookmarks/{id}" should "return a bookmark" in {
    val bookmarkId = mongoCreateBookmark("http://example.org")
    val profileId = mongoProfileId("testuser")

    val url = s"$bookmarksBaseUrl/bookmarks/$bookmarkId"
    val bookmark = (When getTo url withHeaders testuserTokenHeader expect() code SC_OK).withBody[vo.Bookmark]

    bookmark.profile_id shouldBe profileId
    bookmark.url shouldBe "http://example.org"
  }

  "GET /bookmarks/{id}" should "give 404 if a bookmark does not exist" in {
    val url = s"$bookmarksBaseUrl/bookmarks/$randomId"
    When getTo url withHeaders testuserTokenHeader expect() code SC_NOT_FOUND
  }

  "POST /bookmarks" should "give 401 with invalid bearer token" in {
    val url = s"$bookmarksBaseUrl/bookmarks"
    val json = """{"url": "http://example.org", "rating": 7}"""
    val headers = Seq((Authorization.name, s"Bearer ABCDEF"))
    When postTo url entity json withHeaders headers expect() code SC_UNAUTHORIZED
  }

  "POST /bookmarks" should "create a new bookbark" in {
    val profileId = mongoProfileId("testuser")

    val url = s"$bookmarksBaseUrl/bookmarks"
    val json = """{"url": "http://example.org", "rating": 7}"""
    val bookmark = (When postTo url entity json withHeaders testuserTokenHeader expect() code SC_CREATED).withBody[vo.Bookmark]

    bookmark.profile_id shouldBe profileId
    bookmark.url shouldBe "http://example.org"
    bookmark.rating shouldBe 7
  }

  "POST /bookmarks" should "give 409 if the same bookmark already exists" in {
    mongoCreateBookmark("http://example.org")
    val profileId = mongoProfileId("testuser")

    val url = s"$bookmarksBaseUrl/bookmarks"
    val json = """{"url": "http://example.org", "rating": 7}"""
    When postTo url entity json withHeaders testuserTokenHeader expect() code SC_CONFLICT
  }

  "PATCH /bookmarks/{id}" should "give 401 with invalid bearer token" in {
    val url = s"$bookmarksBaseUrl/bookmarks/$randomId"
    val json = """{"url": "http://example.io"}"""
    val headers = Seq((Authorization.name, s"Bearer ABCDEF"))
    When patchTo url entity json withHeaders headers expect() code SC_UNAUTHORIZED
  }

  "PATCH /bookmarks/{id}" should "return a bookmark" in {
    val bookmarkId = mongoCreateBookmark("http://example.org", rating = 7)
    val profileId = mongoProfileId("testuser")

    val url = s"$bookmarksBaseUrl/bookmarks/$bookmarkId"
    val json = """{"url": "http://example.io"}"""
    val bookmark = (When patchTo url entity json withHeaders testuserTokenHeader expect() code SC_OK).withBody[vo.Bookmark]

    bookmark.profile_id shouldBe profileId
    bookmark.url shouldBe "http://example.io"
    bookmark.rating shouldBe 7
  }

  "PATCH /bookmarks/{id}" should "give 404 fs a bookmark does not exist" in {
    val url = s"$bookmarksBaseUrl/bookmarks/$randomId"
    val json = """{"url": "http://example.io"}"""
    When patchTo url entity json withHeaders testuserTokenHeader expect() code SC_NOT_FOUND
  }

  "PATCH /bookmarks/{id}" should "give 403 if a bookmark does not belong to a user" in {
    val bookmarkId = mongoCreateBookmark("http://example.org", rating = 7)

    val url = s"$bookmarksBaseUrl/bookmarks/$bookmarkId"
    val json = """{"url": "http://example.io"}"""
    val headers = authHeaderSeq("testuser2")
    When patchTo url entity json withHeaders headers expect() code SC_FORBIDDEN
  }

  "GET /bookmarks" should "give 401 with invalid bearer token" in {
    val url = s"$bookmarksBaseUrl/bookmarks"
    val headers = Seq((Authorization.name, s"Bearer ABCDEF"))
    When getTo url withHeaders headers expect() code SC_UNAUTHORIZED
  }

  "GET /bookmarks" should "list bookmarks which belong to a user" in {
    val bookmarkIdA = mongoCreateBookmark("http://example.org")
    val bookmarkIdB = mongoCreateBookmark("http://example.io")
    val bookmarkIdC = mongoCreateBookmark("http://example.com", username = "testuser2")
    val profileId = mongoProfileId("testuser")

    val url = s"$bookmarksBaseUrl/bookmarks"
    val bookmarks = (When getTo url withHeaders testuserTokenHeader expect() code SC_OK).withBody[Seq[vo.Bookmark]]

    bookmarks.map(_.bookmark_id) should contain only (bookmarkIdA, bookmarkIdB)
  }

  "DELETE /bookmarks/{id}" should "give 401 with invalid bearer token" in {
    val url = s"$bookmarksBaseUrl/bookmarks/$randomId"
    val headers = Seq((Authorization.name, s"Bearer ABCDEF"))
    When deleteTo url withHeaders headers expect() code SC_UNAUTHORIZED
  }

  "DELETE /bookmarks/{id}" should "delete a bookmark" in {
    val bookmarkIdA = mongoCreateBookmark("http://example.org")
    val bookmarkIdB = mongoCreateBookmark("http://example.io")
    val profileId = mongoProfileId("testuser")

    val urlA = s"$bookmarksBaseUrl/bookmarks/$bookmarkIdA"
    When deleteTo urlA withHeaders testuserTokenHeader expect() code SC_OK

    val urlB = s"$bookmarksBaseUrl/bookmarks"
    val bookmarks = (When getTo urlB withHeaders testuserTokenHeader expect() code SC_OK).withBody[Seq[vo.Bookmark]]

    bookmarks.map(_.bookmark_id) should contain only bookmarkIdB
  }

  "DELETE /bookmarks/{id}" should "give 404 fs a bookmark does not exist" in {
    val url = s"$bookmarksBaseUrl/bookmarks/$randomId"
    When deleteTo url withHeaders testuserTokenHeader expect() code SC_NOT_FOUND
  }

  "DELETE /bookmarks/{id}" should "give 403 if a bookmark does not belong to a user" in {
    val bookmarkId = mongoCreateBookmark("http://example.org", rating = 7)

    val url = s"$bookmarksBaseUrl/bookmarks/$bookmarkId"
    val headers = authHeaderSeq("testuser2")
    When deleteTo url withHeaders headers expect() code SC_FORBIDDEN
  }

}

