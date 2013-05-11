package com.orchid.node.http

import io.netty.channel._
import org.jboss.resteasy.spi.{AsynchronousResponse, ResteasyDeployment}
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.ServletHolder
import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher
import org.eclipse.jetty.webapp.WebAppContext

import javax.ws.rs.{Consumes, Produces, GET, Path}
import org.jboss.resteasy.annotations.Suspend
import javax.ws.rs.core.{MediaType, Response}
import com.orchid.node.rest.{RestServicesApi, RestServicesComponentApi}
import com.fasterxml.jackson.databind.{MapperFeature, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import javax.ws.rs.ext.{ContextResolver, Provider}
import com.fasterxml.jackson.core.JsonFactory.Feature

trait HttpServerConfig{
  def incomingPort:Int
}

trait HttpServerComponentApi{
  def httpServer: HttpServerApi

  def httpServerConfig: HttpServerConfig
}

trait HttpServerApi{
  def start()
}

trait NettyHttpServerComponent extends HttpServerComponentApi{
  self: RestServicesComponentApi =>

  lazy val httpServer = new NettyHttpServer(httpServerConfig, restServices)
}

class NettyHttpServer(config: HttpServerConfig, restServices: RestServicesApi) extends HttpServerApi{
   def start(){
     val deployment = new ResteasyDeployment();

//     val om = deployment.getProviderFactory.injectedInstance(classOf[ObjectMapper])
//     om.registerModule(DefaultScalaModule)
     deployment.getProviders.add(new JacksonContextResolver)
     deployment.getResources.add(restServices)

     val server = new Server(config.incomingPort)
     val context = new WebAppContext();
     context.setContextPath("/")
     context.setResourceBase("/")
     context.addServlet(new ServletHolder(new HttpServletDispatcher()), "/*");
     context.setAttribute(classOf[ResteasyDeployment].getName(), deployment);
     server.setHandler(context)
     server.setStopAtShutdown(true);
     server.start();
   }
}

@Provider
@Produces(value=Array(MediaType.APPLICATION_JSON))
@Consumes(value=Array(MediaType.APPLICATION_JSON))
class JacksonContextResolver extends ContextResolver[ObjectMapper] {

  val objectMapper = {
    val om = new ObjectMapper();
    om.registerModule(new DefaultScalaModule)
    om
  }

  def getContext(`type`: Class[_]): ObjectMapper = {
    objectMapper
  }
}
