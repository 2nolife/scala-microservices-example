package com.coldcore.favorites
package test

import com.mongodb.casbah.Imports._
import org.apache.http.HttpStatus._
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FlatSpec, Matchers}
import ms.auth.vo

class MsAuthSpec extends FlatSpec with BeforeAndAfterAll with Matchers with HelperObjects {

  override protected def beforeAll() {
    systemStart()

    mongoDropDatabase()

    mongoSetupTestUser()
  }

  override protected def afterAll() {
    systemStop()
  }

  "POST to /auth/token" should "give 401 with invalid credentials" in {
    val url = s"$authBaseUrl/auth/token"
    val json = """{"username": "testuser", "password": "mypass"}"""
    When postTo url entity json expect() code SC_UNAUTHORIZED
  }

  "POST to /auth/token" should "create a token" in {
    val url = s"$authBaseUrl/auth/token"
    val json = """{"username": "testuser", "password": "testpass"}"""
    val token = (When postTo url entity json expect() code SC_CREATED).withBody[vo.Token]

    token.username shouldBe "testuser"
    token.access_token.size should be > 10
  }

  "GET to /auth/token?access_token={?}" should "give 401 with invalid access_token parameter" in {
    val url = s"$authBaseUrl/auth/token?access_token=123"
    When getTo url expect() code SC_UNAUTHORIZED
  }

  "GET to /auth/token?access_token={?}" should "return a token" in {
    val url = s"$authBaseUrl/auth/token?access_token=Testuser_BearerToken"
    val token = (When getTo url expect() code SC_OK).withBody[vo.Token]

    token.username shouldBe "testuser"
    token.access_token.size should be > 10
  }

}
