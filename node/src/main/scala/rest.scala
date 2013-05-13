package com.orchid.node.rest

import javax.ws.rs._
import com.orchid.node.rest.Cookie
import java.io.{StringWriter, InputStream}
import org.apache.commons.io.IOUtils

//import org.jboss.resteasy.annotations.Suspend
//import org.jboss.resteasy.spi.AsynchronousResponse
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

  @PUT
  @Path(value="/files{path:.*}")
  def filePut(@PathParam("path") path: String, i:InputStream) = {
    val writer = new StringWriter();
    IOUtils.copy(i, writer, "UTF-8");
    val string = writer.toString

    println("OLOLOL " + string)
  }

  @GET
  @Path(value="/files")
  @Produces(value=Array(MediaType.APPLICATION_JSON))
  def fileGet = handlePath("")

  def handlePath(path:String)={
    Response.ok(Cookie("path:"+path)).build()
  }

}