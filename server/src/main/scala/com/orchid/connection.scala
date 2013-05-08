package com.orchid.connection

import com.orchid.user.UserID
import java.util.UUID
import com.orchid.table.{SimpleIndex, Table}

case class ClientPrincipal(id:UUID)

case class ClientData(principal: ClientPrincipal, connection: UserID, port:Int){
  def clientActive = !connection.getConnection.isDisposed
}

trait ConnectionComponentApi{
  def connectionApi:ConnectionApi
}

trait ConnectionApi{
  def sessions:Table[ClientData]
  def sessionByConnection(connection:UserID):Option[ClientData]
  def sessionbyPrincipal(principal:ClientPrincipal):Option[ClientData]
  def connectUser(principal: ClientPrincipal, connection:UserID, port:Int):Unit
  def cleanupUser(connection: UserID):Unit
}

trait ConnectionComponent extends ConnectionComponentApi{
  val connectionApi:ConnectionApi = new ConnectionApi{
    val byConnectionIndex = SimpleIndex[ClientData](_.connection)
    val byPrincipalIndex = SimpleIndex[ClientData](_.principal)

    @volatile
    var sessions = Table[ClientData]()
      .withIndex(byConnectionIndex)
      .withIndex(byPrincipalIndex)

    def byConnection = sessions(byConnectionIndex)
    def byPrincipal = sessions(byPrincipalIndex)

    def sessionByConnection(connection: UserID): Option[ClientData] = byConnection(connection).headOption
    def sessionbyPrincipal(principal: ClientPrincipal): Option[ClientData] = byPrincipal(principal).headOption

    def connectUser(principal: ClientPrincipal, connection:UserID, port:Int){
      val cleanedSessions = byConnection.get(connection).map(sessions-_.head).getOrElse(sessions);
      sessions = cleanedSessions + ClientData(principal, connection, port)
    }

    def cleanupUser(connection: UserID){
      for (session <- byConnection(connection).headOption){
        sessions -= session
      }
    }
  }
}