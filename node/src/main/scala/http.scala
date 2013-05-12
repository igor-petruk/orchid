package com.orchid.node.http

import org.jboss.resteasy.spi.{ResteasyDeployment}
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.{FilterHolder, ServletHolder}
import org.jboss.resteasy.plugins.server.servlet.{Filter30Dispatcher, HttpServlet30Dispatcher}
import org.eclipse.jetty.webapp.WebAppContext

import javax.ws.rs.{Consumes, Produces}
import javax.ws.rs.core.{MediaType}
import com.orchid.node.rest.{RestServicesApi, RestServicesComponentApi}
import com.fasterxml.jackson.databind.{ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import javax.ws.rs.ext.{ContextResolver, Provider}
import org.eclipse.jetty.util.resource.Resource
import java.util
import javax.servlet.DispatcherType

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

     deployment.getProviders.add(new ScalaEnabledObjectMapperContextResolver)
     deployment.getResources.add(restServices)

     val server = new Server(config.incomingPort)
     val context = new WebAppContext();
     context.setContextPath("/")
     context.setBaseResource(Resource.newClassPathResource("/www"))
     context.addServlet(new ServletHolder(new HttpServlet30Dispatcher()), "/rest/*");
     context.addFilter(new FilterHolder(new Filter30Dispatcher()),"/www/*",
       util.EnumSet.of(DispatcherType.ASYNC,DispatcherType.FORWARD, DispatcherType.REQUEST))
     context.setAttribute(classOf[ResteasyDeployment].getName(), deployment);
     server.setHandler(context)
     server.setStopAtShutdown(true);
     server.start();
   }
}

@Provider
@Produces(value=Array(MediaType.APPLICATION_JSON))
@Consumes(value=Array(MediaType.APPLICATION_JSON))
class ScalaEnabledObjectMapperContextResolver extends ContextResolver[ObjectMapper] {

  val objectMapper = {
    val om = new ObjectMapper();
    om.registerModule(new DefaultScalaModule)
    om
  }

  def getContext(`type`: Class[_]): ObjectMapper = {
    objectMapper
  }
}
