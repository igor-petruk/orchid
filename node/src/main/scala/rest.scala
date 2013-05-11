package com.orchid.node.rest

import javax.ws.rs.{Produces, GET, Path}
import org.jboss.resteasy.annotations.Suspend
import org.jboss.resteasy.spi.AsynchronousResponse
import javax.ws.rs.core.{MediaType, Response}
import beans.BeanProperty

trait RestServicesComponentApi{
  def restServices:RestServicesApi
}

trait RestServicesApi{ }

trait JaxRsRestServicesComponent extends RestServicesComponentApi{
  lazy val restServices: RestServicesApi = new RestServices()
}

case class Cookie(name:String)

@Path(value="/")
class RestServices extends RestServicesApi{
  @GET
  @Path(value="/hi")
  @Produces(value=Array(MediaType.APPLICATION_JSON))
  def cookieCookie = Cookie("test")

}