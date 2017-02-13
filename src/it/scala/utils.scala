package com.coldcore.favorites
package test

import akka.actor.ActorSystem
import akka.http.scaladsl.model.headers.Authorization
import ms.http.RestClient.{HttpCall, HttpCallSuccessful}
import ms.http.RestClient
import ms._
import ms.db.MongoQueries
import ms.auth.Constants.APP
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.casbah.{MongoClient, MongoCollection}
import com.mongodb.casbah.Imports._
import spray.json.{JsObject, _}
import org.apache.http.HttpStatus._
import org.bson.types.ObjectId
import org.scalatest.exceptions.TestFailedException

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration

/** Common trait to include into the integration tests. */
trait HelperObjects extends MongoOps with SystemStart with RestClientOps with RestClientDSL with FilesReader {

  implicit def string2jso(s: String): JsObject = s.parseJson.asJsObject

}

/** Read files from resources directory. */
trait FilesReader {

  def readFileAsString(path: String): String = {
    val stream = getClass.getResourceAsStream(path)
    scala.io.Source.fromInputStream(stream).mkString
  }

}

/** REST client and authorization headers. */
trait RestClientOps {

  val restClient = new RestClient

  val authHeaderSeq = (username: String) =>
    Seq((Authorization.name, s"Bearer ${username.capitalize}_BearerToken"))

  val testuserTokenHeader = authHeaderSeq("testuser")
  val systemTokenHeader = Seq((Authorization.name, s"Bearer MySystemToken"))

  /** Check a response code and return its body:
    *   restClient.post(url, json, Authorization.name, "Bearer ABCDEF").expectWithCode(SC_CREATED).convertTo[TokenRemote]
    */
  implicit class ExpectedResponse(httpCall: HttpCall) {
    def expectWithCode(code: Int): JsValue = httpCall match {
      case HttpCallSuccessful(_, c, body, _) if c == code => body
      case x => throw new TestFailedException(s"Received: $x", 4)
    }
  }

}

/** Start and stop the system. */
trait SystemStart extends BaseURLs {

  implicit val system = ActorSystem("favorites")

  sys.addShutdownHook {
    println("Shutting down ...")
    system.terminate()
  }

  def systemStart() {
    auth.start.run
    profiles.start.run
    bookmarks.start.run

    // wait till all micro services start
    val restClient = new RestClient
    implicit val executionContext = system.dispatcher
    def started: Boolean = baseUrls.map(url => isStarted(url, restClient)).forall(true==)
    Await.result(Future { while (!started) Thread.sleep(100) }, 20 seconds)
  }

  def systemStop() {
    system.terminate()
    Await.result(system.whenTerminated, Duration.Inf)
  }

  private def isStarted(baseUrl: String, restClient: RestClient): Boolean =
    restClient.get(s"$baseUrl/heartbeat") match {
      case HttpCallSuccessful(_, SC_OK, _, _) => true
      case _ => false
    }

}

/** Micro services base URLs. */
trait BaseURLs {

  val authBaseUrl = "http://localhost:8022"
  val profilesBaseUrl = "http://localhost:8021"
  val bookmarksBaseUrl = "http://localhost:8023"

  val baseUrls =
    authBaseUrl :: profilesBaseUrl :: bookmarksBaseUrl :: Nil
}

/** MongoDB client and operations. */
trait MongoOps extends MongoQueries with MongoTables with MongoUsers with MongoCreate with MongoCleaner {
  lazy val mongoClient = MongoClient("localhost", 27017)
  lazy val mongoDB = mongoClient("favorites-test")

  def randomId: String = ObjectId.get.toString
}

/** MongoDB collections. */
trait MongoTables {
  self: MongoOps =>

  lazy val mongoAuthUsers: MongoCollection = mongoDB(s"${auth.Constants.MS}-users")
  lazy val mongoAuthTokens: MongoCollection = mongoDB(s"${auth.Constants.MS}-tokens")
  lazy val mongoProfiles: MongoCollection = mongoDB(profiles.Constants.MS)
  lazy val mongoBookmarks: MongoCollection = mongoDB(bookmarks.Constants.MS)
}

/** MongoDB users setup. */
trait MongoUsers {
  self: MongoOps =>

  def mongoSetupTestUser() {
    mongoRemoveTestUser()
    mongoSetupUser("testuser")
  }

  def mongoSetupUser(username: String) {
    mongoRemoveUser(username)

    mongoAuthUsers.update(
      "username" $eq username,
      MongoDBObject(
        "test" -> true,
        "username" -> username,
        "password" -> "testpass"),
      upsert = true
    )
    mongoAuthTokens.update(
      "username" $eq username,
      MongoDBObject(
        "test" -> true,
        "username" -> username,
        "token" -> s"${username.capitalize}_BearerToken",
        "type" -> "Bearer",
        "expires" -> (3600*1000L+System.currentTimeMillis)),
      upsert = true
    )
    mongoProfiles.update(
      "username" $eq username,
      MongoDBObject(
        "test" -> true,
        "username" -> username,
        "email" -> s"$username@example.org"),
      upsert = true
    )
  }

  def mongoRemoveTestUser() = mongoRemoveUser("testuser")

  def mongoRemoveUser(username: String) {
    mongoAuthUsers.findAndRemove("username" $eq username)
    mongoAuthTokens.findAndRemove("username" $eq username)
    mongoProfiles.findAndRemove("username" $eq username)
  }

  def mongoProfileId(username: String): String =
    mongoProfiles
      .findOne("username" $eq username)
      .map(_.getAs[ObjectId]("_id").get)
      .getOrElse(ObjectId.get)
      .toString

}

/** Create objects in MongoDB. */
trait MongoCreate {
  self: MongoOps =>

  def mongoCreateBookmark(url: String, rating: Int = 0, username: String = "testuser"): String = {
    val place = MongoDBObject(
      "test" -> true,
      "profile_id" -> mongoProfileId(username),
      "url" -> url,
      "rating" -> rating)
    mongoBookmarks
      .insert(place)
    place.idString
  }

}

/** Delete objects from MongoDB. */
trait MongoCleaner {
  self: MongoOps =>

  def mongoDropDatabase() =
    mongoDB.dropDatabase()

  def mongoRemoveBookmarks(username: String = "testuser") {
    val findByProfileId = "profile_id" $eq mongoProfileId(username)
    mongoBookmarks
      .findAndRemove(findByProfileId)
  }

}

/** REST DSL
  * Examples:
  *   When.postTo(url).entity(json).withHeaders(headers).expect.code(SC_CREATED).withBody[TokenRemote]
  *   val token = (When postTo url entity json withHeaders headers expect() code SC_CREATED).withBody[TokenRemote]
  *   When postTo url entity json withHeaders headers expect() code SC_CREATED
  */
trait RestClientDSL {

  private val restClient = new RestClient
  implicit private def string2jso(s: String): JsObject = s.parseJson.asJsObject

  case class Values(method: Symbol = null, url: String = null, json: JsObject = null,
                    headers: Seq[(String,String)] = Nil,
                    restCallResult: Option[HttpCall] = None)

  case object When {
    def postTo(url: String): Post = Post(Values(url = url, method = 'post))
    def getTo(url: String): Get = Get(Values(url = url, method = 'get))
    def putTo(url: String): Put = Put(Values(url = url, method = 'put))
    def deleteTo(url: String): Delete = Delete(Values(url = url, method = 'delete))
    def patchTo(url: String): Patch = Patch(Values(url = url, method = 'patch))
  }

  case class Post(values: Values) {
    def entity(json: JsObject): Entity = Entity(values.copy(json = json))
  }

  case class Put(values: Values) {
    def entity(json: JsObject): Entity = Entity(values.copy(json = json))
  }

  case class Patch(values: Values) {
    def entity(json: JsObject): Entity = Entity(values.copy(json = json))
  }

  case class Get(values: Values) {
    def expect(): Expect = Expect(values)
    def withHeaders(headers: Seq[(String,String)]): Headers = Headers(values.copy(headers = headers))
  }

  case class Delete(values: Values) {
    def expect(): Expect = Expect(values)
    def withHeaders(headers: Seq[(String,String)]): Headers = Headers(values.copy(headers = headers))
  }

  case class Entity(values: Values) {
    def expect(): Expect = Expect(values)
    def withHeaders(headers: Seq[(String,String)]): Headers = Headers(values.copy(headers = headers))
  }

  case class Headers(values: Values) {
    def expect(): Expect = Expect(values)
  }

  case class Expect(values: Values) {
    import values._
    lazy val httpCall =
      restCallResult.getOrElse {
        values.method match {
          case 'post => restClient.post(url, json, headers: _*)
          case 'put => restClient.put(url, json, headers: _*)
          case 'patch => restClient.patch(url, json, headers: _*)
          case 'get => restClient.get(url, headers: _*)
          case 'delete => restClient.delete(url, headers: _*)
        }
      }
    lazy val nvalues = values.copy(restCallResult = Some(httpCall))

    def code(code: Int): Expect =
      httpCall match {
        case HttpCallSuccessful(_, c, _, _) if c == code => Expect(nvalues)
        case x => throw new TestFailedException(s"Received: $x", 4)
      }

    def withBody[T : JsonReader]: T =
      httpCall match {
        case HttpCallSuccessful(_, _, body, _) => body.convertTo[T]
        case x => throw new TestFailedException(s"Received: $x", 4)
      }
  }

}