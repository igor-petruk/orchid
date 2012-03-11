package com.orchid.main

import com.orchid.tree.FilesystemTreeComponent
import com.orchid.HandlersComponent
import com.orchid.logic.FlowConnectorComponent

/**
 * User: Igor Petruk
 * Date: 04.03.12
 * Time: 20:58
 */

abstract class App extends FilesystemTreeComponent
                  with FlowConnectorComponent
                  with HandlersComponent{
  def start{
    flow.start()
  }
}

object Runner {


  def main(argv: Array[String]) {
    val app = new App{
      def port = 9800
    }
    app.start
  }
}
