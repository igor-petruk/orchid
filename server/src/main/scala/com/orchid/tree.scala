package com.orchid.tree

import java.util.UUID
import collection._
import annotation.tailrec
import com.orchid.user.UserID
import com.orchid.messages.generated.Messages.ErrorType
import concurrent.TrieMap
import com.orchid.connection.{ConnectionComponentApi, ConnectionApi, ClientPrincipal}
import com.orchid.table.{SimpleIndex, Table}

/**
 * User: Igor Petruk
 * Date: 07.03.12
 * Time: 22:29
 */

case class FilesystemError(errorType:ErrorType, description:Option[String]=None)

object Node{
  val rootNodeUUID = new UUID(0,0)
}

case class Node(
  id:UUID,
  name:String,
  isDir:Boolean,
  size:Long,
  children:Map[String, Node],
  childrenCount:Int=1){

  def withChildren(newChildren: Map[String, Node],childrenCount:Int)=
    Node(id, name, isDir, size, newChildren,childrenCount)

  def isRoot = id == Node.rootNodeUUID
}

trait FilesystemTree {
  def root:Node
  def file(path:String):Option[Node]
  def file(id:UUID):Option[Node]
  def setFile(parent: String, child:Node):Either[FilesystemError, Node]
  def discoverFile(id:UUID, principal: ClientPrincipal):Boolean
  def nodesById:Map[AnyRef,Set[NodeInfo]]
}

trait FilesystemTreeComponentApi {
  val filesystem:FilesystemTree
}

case class NodeInfo(node:Node, peers:Set[ClientPrincipal])

trait FilesystemTreeComponent extends FilesystemTreeComponentApi{
  self: ConnectionComponentApi =>

  val filesystem = new FilesystemTreeImpl

  class FilesystemTreeImpl extends FilesystemTree{

    @volatile
    var rootNode:Node = Node(Node.rootNodeUUID,"ROOT",true, 0,
      immutable.HashMap[String, Node](),0)

    val nodesByIdIndex = SimpleIndex[NodeInfo](_.node.id)
    @volatile
    var nodesInfo = Table[NodeInfo]().withIndex(nodesByIdIndex)
    def nodesById = nodesInfo(nodesByIdIndex)

    def root = rootNode

    def discoverFile(id:UUID, peer: ClientPrincipal):Boolean={
      nodesById(id).headOption match {
        case Some(node) => {
          nodesInfo = nodesInfo - node + node.copy(peers=node.peers+peer)
          true
        }
        case None => false
      }
    }

    def dumpTree{
      def dumpNode(offset:String, node:Node){
        val dirName = (if (node.isDir) "[%s]" else "%s").format(node.name)
        println("%s%s\t\t\t%d".format(offset,dirName,node.childrenCount))
        node.children.values.foreach(child=>dumpNode(offset+"  ",child))
      }
      dumpNode("",root)
    }

    def file(id:UUID)= nodesById(id).headOption.map(_.node)

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

    def setFile(parent: String, child:Node):Either[FilesystemError,Node]={
      def setFileAsChild(node:Node, names: List[String]):Either[FilesystemError,Node]={
        if (names.isEmpty){
          if (node.children.contains(child.name))
            Left(FilesystemError(ErrorType.FILE_EXISTS))
          else
            Right(node.withChildren(node.children + (child.name->child), node.childrenCount+child.childrenCount))
        } else
          node.children.get(names.head) match { 
            case Some(oldChild)=>
              setFileAsChild(oldChild, names.tail) match {
                case Right(newChild)=> Right(node.withChildren(node.children + 
                  (names.head->newChild),node.childrenCount+newChild.childrenCount-oldChild.childrenCount))
                case error@Left(_)=>error
              }
            case None=>Left(
              FilesystemError(ErrorType.FILE_NOT_FOUND)
            )
          }
      }

      val pathList = if (parent.isEmpty) List.empty
      else parent.split('/').filter(!_.isEmpty).toList

      setFileAsChild(root, pathList) match {
        case Right(newRoot)=>
          nodesInfo += NodeInfo(child,Set())
          rootNode = newRoot
          Right(child)
        case error@Left(_) => error
      }
    }
  }
}

