package com.coldcore.favorites
package ms.auth.vo

import spray.json.DefaultJsonProtocol

case class Credentials(username: String, password: String)
object Credentials extends DefaultJsonProtocol { implicit val format = jsonFormat2(apply) }

case class Token(access_token: String, token_type: String, expires: Int, username: String)
object Token extends DefaultJsonProtocol { implicit val format = jsonFormat4(apply) }
