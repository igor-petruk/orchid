package com.orchid.actors

import akka.actor.ActorSystem

trait AkkaActorsComponentApi {
  def actorSystem:ActorSystem
}

trait AkkaActorsComponent extends AkkaActorsComponentApi{
  lazy val actorSystem = ActorSystem()
}
