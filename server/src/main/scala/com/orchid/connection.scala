package com.orchid.connection

import com.orchid.user.UserID

case class ClientPrincipal(name:String)

case class ClientData(connection: UserID){
  def clientActive = !connection.getConnection.isDisposed
}

trait ConnectionComponentApi{
  def connectionApi:ConnectionApi
}

trait ConnectionApi{
  def sessions:Map[ClientPrincipal, ClientData]
}

trait ConnectionComponent extends ConnectionComponentApi{
  val connectionApi:ConnectionApi = new ConnectionApi{
    @volatile
    var sessions = Map[ClientPrincipal, ClientData]()

    def connectUser(principal: ClientPrincipal, connection:UserID){
      sessions += (principal->ClientData(connection))
    }

    def disconnectUser(principal: ClientPrincipal){
      sessions -= principal
    }
  }
}