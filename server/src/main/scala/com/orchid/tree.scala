package com.orchid.tree

import java.util.UUID
import collection._
import annotation.tailrec
import com.orchid.user.UserID
import com.orchid.messages.generated.Messages.ErrorType
import concurrent.TrieMap

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
  def discoverFile(id:UUID, peer: UserID):Boolean
}

trait FilesystemTreeComponentApi {
  val filesystem:FilesystemTree
}

trait FilesystemTreeComponent extends FilesystemTreeComponentApi{
  val filesystem = new FilesystemTreeImpl

  case class NodePeers(node:Node, peers:List[UserID])

  class FilesystemTreeImpl extends FilesystemTree{

    var rootNode:Node = Node(Node.rootNodeUUID,"ROOT",true, 0,
      immutable.HashMap[String, Node](),0)

    val nodesById:mutable.Map[UUID, NodePeers]= TrieMap.empty

    def root = rootNode

    def discoverFile(id:UUID, peer: UserID):Boolean={
      nodesById.get(id) match {
        case Some(oldPeers) => {
          nodesById += (id->NodePeers(oldPeers.node, peer::oldPeers.peers))
          true
        }
        case _ => false
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

    def file(id:UUID)= nodesById.get(id).map(_.node)

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
          nodesById+=(child.id->NodePeers(child, List()))
          rootNode = newRoot
          Right(child)
        case error@Left(_) => error
      }
    }
  }
}

