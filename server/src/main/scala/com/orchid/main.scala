package com.orchid.main

import com.orchid.tree.{FilesystemTreeComponent}
import com.orchid.logic.{BusinessLogicComponent}
import com.orchid.journal.JournalComponent
import com.orchid.flow.{HandlersComponent, FlowConnectorComponent}

/**
 * User: Igor Petruk
 * Date: 04.03.12
 * Time: 20:58
 */

abstract class MainComponentBusinessLogic extends FilesystemTreeComponent
                  with JournalComponent
                  with BusinessLogicComponent
                  with HandlersComponent
                  with FlowConnectorComponent{
  def start{
    flow.start()
  }
}

object Runner {


  def main(argv: Array[String]) {
    val app = new MainComponentBusinessLogic{
      def host = "localhost"
      def port = 9800
    }
    app.start
  }
}
