package com.coldcore.favorites
package ms.http

import akka.http.scaladsl.model.headers.Authorization
import org.apache.http.HttpStatus._
import org.apache.http.client.HttpClient
import org.apache.http.client.methods._
import org.apache.http.entity.{ContentType, StringEntity}
import org.apache.http.impl.client.HttpClientBuilder
import spray.json._

import scala.io.Source

object RestClient {
  sealed trait HttpCall
  case class HttpCallSuccessful(url: String, code: Int, json: JsValue, rh: Seq[(String,String)]) extends HttpCall
  case class HttpCallFailed(url: String, error: Throwable) extends HttpCall

  case class ResponseContent(content: String)
  object ResponseContent extends DefaultJsonProtocol { implicit val format = jsonFormat1(apply) }

}

class RestClient {
  import RestClient._

  private val client = HttpClientBuilder.create.build

  private def doHttpCall(url: String, client: HttpClient, method: HttpRequestBase): HttpCall = {
    try {
      val response = client.execute(method)
      val content = Source.fromInputStream(response.getEntity.getContent).mkString
      val json =
        try { content.parseJson } catch {
          case _: Throwable => ResponseContent(content).toJson
        }

      HttpCallSuccessful(url, response.getStatusLine.getStatusCode, json,
        response.getAllHeaders.map(x => (x.getName, x.getValue)))
    } catch {
      case e : Throwable => HttpCallFailed(url, e)
    } finally {
      method.releaseConnection()
    }
  }

  /** HTTP {method} as "application/json", overwrite with "rh" as "Content-type", "text/xml; charset=UTF-8" */
  private def post_put_patch(method: HttpEntityEnclosingRequestBase, url: String, json: JsObject, rh: Seq[(String,String)]): HttpCall = {
    method.setEntity(new StringEntity(json.toString, ContentType.create("application/json", "UTF-8")))
    for ((name, value) <- rh) method.setHeader(name, value)
    doHttpCall(url, client, method)
  }

  private def get_delete(method: HttpRequestBase, url: String, rh: Seq[(String,String)]): HttpCall = {
    for ((name, value) <- rh) method.setHeader(name, value)
    doHttpCall(url, client, method)
  }

  def close() =
    client.close()

  def get(url: String, rh: (String,String)*): HttpCall =
    get_delete(new HttpGet(url), url, rh)

  def delete(url: String, rh: (String,String)*): HttpCall =
    get_delete(new HttpDelete(url), url, rh)

  def post(url: String, json: JsObject, rh: (String,String)*): HttpCall =
    post_put_patch(new HttpPost(url), url, json, rh)

  def put(url: String, json: JsObject, rh: (String,String)*): HttpCall =
    post_put_patch(new HttpPut(url), url, json, rh)

  def patch(url: String, json: JsObject, rh: (String,String)*): HttpCall =
    post_put_patch(new HttpPatch(url), url, json, rh)

  implicit def obj2json[T : JsonWriter](obj: T): JsObject = obj.toJson.asJsObject

  implicit class HttpCallX(call: HttpCall) {
    def codeWithBody[T : JsonReader]: (Int, Option[T]) =
      call match {
        case HttpCallSuccessful(_, code @ (SC_OK | SC_CREATED), body, _) => (code, Some(body.convertTo[T]))
        case HttpCallSuccessful(_, code, _, _) => (code, None)
        case _ => (SC_INTERNAL_SERVER_ERROR, None)
      }

    def code: Int =
      call match {
        case HttpCallSuccessful(_, code, _, _) => code
        case _ => SC_INTERNAL_SERVER_ERROR
      }
  }

  private val header = (token: String) => (Authorization.name, s"Bearer $token")

  def restGet[T : JsonReader](url: String): (Int, Option[T]) =
    get(url).codeWithBody

  def restGet[T : JsonReader](url: String, token: String): (Int, Option[T]) =
    get(url, header(token)).codeWithBody

  def restDelete[T : JsonReader](url: String, token: String): (Int, Option[T]) =
    delete(url, header(token)).codeWithBody

  def restPost[T : JsonReader](url: String, obj: JsObject, token: String): (Int, Option[T]) =
    post(url, obj, header(token)).codeWithBody

  def restPut[T : JsonReader](url: String, obj: JsObject, token: String): (Int, Option[T]) =
    put(url, obj, header(token)).codeWithBody

  def restPatch[T : JsonReader](url: String, obj: JsObject, token: String): (Int, Option[T]) =
    patch(url, obj, header(token)).codeWithBody

}

