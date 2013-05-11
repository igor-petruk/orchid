package com.orchid.node.akka2

import akka.actor.ActorSystem

trait AkkaSystem{
  lazy val actorSystem = ActorSystem()

  lazy val trackerClientOperations = actorSystem.dispatchers.lookup("tracker-client").prepare()
}