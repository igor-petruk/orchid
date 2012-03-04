package com.orchid.main

import com.orchid.logging.LoggingModule
import com.orchid.flow.FlowModule
import com.orchid.serialization.ProtobufMessageSerializationModule
import com.orchid.logic.LogicModule
import com.orchid.logic.LogicModule
import com.google.inject.{Guice, Injector, AbstractModule}
import com.orchid.net.server.main.NetworkServer

/**
 * User: Igor Petruk
 * Date: 04.03.12
 * Time: 20:58
 */

object Runner {

  /**
   * User: Igor Petruk
   * Date: 18.02.12
   * Time: 16:20
   */
  class RunnerModule extends AbstractModule {
    def configure {
      install(new LoggingModule)
      install(new FlowModule)
      install(new ProtobufMessageSerializationModule)
      install(new LogicModule)
    }
  }

  def main(argv: Array[String]) {
    val injector = Guice.createInjector(new RunnerModule)
    val server = injector.getInstance(classOf[NetworkServer])
    server.start
  }
}
