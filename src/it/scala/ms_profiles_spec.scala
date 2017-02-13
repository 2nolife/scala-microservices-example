package com.coldcore.favorites
package test

import akka.http.scaladsl.model.headers.Authorization
import org.apache.http.HttpStatus._
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FlatSpec, Matchers}
import ms.profiles.vo

class MsProfilesSpec extends FlatSpec with BeforeAndAfterAll with Matchers with HelperObjects {

  override protected def beforeAll() {
    systemStart()

    mongoDropDatabase()

    mongoSetupTestUser()
    mongoSetupUser("testuser2")
  }

  override protected def afterAll() {
    systemStop()
  }

  "GET /profiles/me" should "give 401 with invalid bearer token" in {
    val url = s"$profilesBaseUrl/profiles/me"
    val headers = Seq((Authorization.name, s"Bearer ABCDEF"))
    When getTo url withHeaders headers expect() code SC_UNAUTHORIZED
  }

  "GET /profiles/me" should "return a profile" in {
    val url = s"$profilesBaseUrl/profiles/me"
    val profile = (When getTo url withHeaders testuserTokenHeader expect() code SC_OK).withBody[vo.Profile]

    profile.username.get shouldBe "testuser"
    profile.email.get shouldBe "testuser@example.org"
  }

  "GET /profiles/{id}" should "give 401 with invalid bearer token" in {
    val url = s"$profilesBaseUrl/profiles/$randomId"
    val headers = Seq((Authorization.name, s"Bearer ABCDEF"))
    When getTo url withHeaders headers expect() code SC_UNAUTHORIZED
  }

  "GET /profiles/{id}" should "give 404 if a profile does not exist" in {
    val url = s"$profilesBaseUrl/profiles/$randomId"
    When getTo url withHeaders testuserTokenHeader expect() code SC_NOT_FOUND
  }

  "GET /profiles/{id}" should "return a profile" in {
    val profileId = mongoProfileId("testuser2")

    val url = s"$profilesBaseUrl/profiles/$profileId"
    val profile = (When getTo url withHeaders testuserTokenHeader expect() code SC_OK).withBody[vo.Profile]

    profile.profile_id shouldBe profileId
    profile.username.get shouldBe "testuser2"
  }

}

