package com.coldcore.favorites
package ms.profiles

import akka.actor.{ExtendedActorSystem, Extension, ExtensionKey}
import com.typesafe.config.Config
import Constants._

object Settings extends ExtensionKey[Settings]

class Settings(val system: ExtendedActorSystem) extends Extension {
  private val config: Config = system.settings.config

  val hostname: String = config.getString(s"$APP.$MS.http_bind_hostname")
  val port: Int = config.getInt(s"$APP.$MS.http_bind_port")

  val mongoDbHostname: String = config.getString(s"$APP.mongodb_hostname")
  val mongoDbPort: Int = config.getInt(s"$APP.mongodb_port")
  val mongoDbName: String = config.getString(s"$APP.mongodb_name")

  val authBaseUrl: String = config.getString(s"$APP.auth_base_url")
}
