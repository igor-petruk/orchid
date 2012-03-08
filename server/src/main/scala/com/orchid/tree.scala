package com.orchid.tree

import com.google.inject.{Singleton, AbstractModule}
import java.util.UUID
import collection.immutable
import annotation.tailrec


/**
 * User: Igor Petruk
 * Date: 07.03.12
 * Time: 22:29
 */

case class Node(
  id:UUID,
  name:String,
  isDir:Boolean,
  children:Map[String, Node]
)

trait FilesystemTree {
  def root:Node
  def file(path:String):Option[Node]
  def setFile(parent: String, child:Node):Unit
}

class FilesystemTreeImpl extends FilesystemTree{
  var rootNode:Node = Node(new UUID(0,0),"ROOT",true,
    immutable.HashMap[String, Node]())

  def root = rootNode

  def file(node:Node, fileName:String):Option[Node]=
    if (node.isDir)
      node.children.get(fileName)
    else
      None
  
  def file(path:String):Option[Node]={
    @tailrec
    def fileIter(node:Node, names: List[String]):Option[Node]={
      if (names.isEmpty) Some(node) else
        file(node, names.head) match {
          case Some(fileFound) => fileIter(fileFound, names.tail)
          case None => None
        }
    }
    fileIter(root, if (path.isEmpty) List.empty else path.split('/').toList)
  }

  def setFile(parent: String, child:Node){
    def setFileAsChild(node:Node, names: List[String]):Option[Node]={
      if (names.isEmpty)
        Some(Node(
          node.id,
          node.name,
          node.isDir,
          node.children + (child.name->child)
        ))
      else 
        node.children.get(names.head) match {
        case Some(oldChild)=>
          val newChild = setFileAsChild(oldChild, names.tail)
          newChild match {
            case Some(newChildNode) => Some(Node(
              node.id,
              node.name,
              node.isDir,
              node.children + (names.head->newChildNode)
            ))
            case _ => None
          }
        case _ => None
      }
    }

    val pathList = if (parent.isEmpty) 
                      List.empty 
                  else 
                      parent.split('/').toList
    
    for (newRoot <- setFileAsChild(root, pathList)){
      rootNode = newRoot
    }
  }
}

class FilesystemTreeModule extends AbstractModule{
  def configure() {
    bind(classOf[FilesystemTree]).to(classOf[FilesystemTreeImpl]).
      in(classOf[javax.inject.Singleton])
  }
}