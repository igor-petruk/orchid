package com.orchid.node.rest

import javax.ws.rs.{PathParam, Produces, GET, Path}
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

@Path(value="/rest")
class RestServices extends RestServicesApi{
  @GET
  @Path(value="/files{path:.*}")
  @Produces(value=Array(MediaType.APPLICATION_JSON))
  def fileGet(@PathParam("path") path: String) = handlePath(path)

  @GET
  @Path(value="/files")
  @Produces(value=Array(MediaType.APPLICATION_JSON))
  def fileGet = handlePath("")

  def handlePath(path:String)={
    Response.ok(Cookie("path:"+path)).build()
  }

}