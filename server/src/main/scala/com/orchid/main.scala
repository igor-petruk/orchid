package com.orchid.main

import com.orchid.tree.{FilesystemTreeComponent}
import com.orchid.logic.{BusinessLogicComponent}
import com.orchid.flow.FlowConnectorComponent

/**
 * User: Igor Petruk
 * Date: 04.03.12
 * Time: 20:58
 */

trait Other

trait Parent{
  def list:List[Int]
}

trait Child extends Parent{
  self: Other=>
  val list = List(2)
}

abstract class MainComponentBusinessLogic extends FilesystemTreeComponent
                  with BusinessLogicComponent
                  with FlowConnectorComponent{
  def start{
    flow.start()
  }
}

object Runner {


  def main(argv: Array[String]) {
    val app = new MainComponentBusinessLogic{
      def port = 9800
    }
    app.start
  }
}
