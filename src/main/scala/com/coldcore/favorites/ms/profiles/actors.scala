package com.coldcore.favorites
package ms.profiles.actors

import akka.actor.{Actor, ActorLogging, Props}
import ms.profiles.db.ProfilesDb
import ms.profiles.vo

object ProfilesActor {
  def props(profilesDb: ProfilesDb): Props = Props(new ProfilesActor(profilesDb))

  case class GetProfileByUsernameIN(username: String)
  case class GetProfileByUsernameOUT(profile: Option[vo.Profile])

  case class GetProfileByIdIN(profileId: String)
  case class GetProfileByIdOUT(profile: Option[vo.Profile])
}

class ProfilesActor(val profilesDb: ProfilesDb) extends Actor with ActorLogging {
  import ProfilesActor._

  def receive = {

    case GetProfileByUsernameIN(username) =>
      sender ! GetProfileByUsernameOUT(profilesDb.profileByUsername(username))

    case GetProfileByIdIN(profileId) =>
      sender ! GetProfileByIdOUT(profilesDb.profileById(profileId))

  }

}
