package com.orchid.authentication

import com.orchid.user.UserID

case class ClientKey(name:String)

case class ClientInfo(key:Option[ClientKey], currentConnection:UserID)
