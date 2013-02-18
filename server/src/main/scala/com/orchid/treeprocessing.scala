package com.orchid.treeprocessing

import com.orchid.tree.{Node, FilesystemTree}
import scala.collection.mutable
import java.util.UUID

import java.io._
import storage.generated.Storage.{SerializedNode, JointPoint, Command}
import java.util.zip.GZIPOutputStream
import com.orchid.actors.AkkaActorsComponentApi
import akka.actor.{ActorRef, Props, Actor}
import akka.event.Logging
import scala.Some
import com.orchid.utils.{XMLUtils, UUIDConversions}

trait TreeSerialization{
  def serialize(filesystem:FilesystemTree)
}

trait TreeSerializationComponentApi{
  def treeSerializer:TreeSerialization
}

case class ChunkMetadata(provides:UUID, depends:Set[UUID])

case class StartSerialization(folder:String, filesystem:FilesystemTree,
                              numberOfParts:Int, originalSender: Option[ActorRef] = None)
case class SerializeChunk(folder:String,topNode:Node, previousJointPointsSnapshot:Set[UUID])
case class SerializationComplete()

class ChunkProcessorActor extends Actor with UUIDConversions{

  def receive={
    case SerializeChunk(folder, topNode, previousJointPointsSnapshot)=>{
      val met = mutable.Set[UUID]()
      val chunkDir = new File(folder+topNode.id)
      chunkDir.mkdirs()
      val binaryFile = chunkDir.getAbsolutePath+File.separatorChar+"bin"
      val textFile = chunkDir.getAbsolutePath+File.separatorChar+"log.txt"
      val binaryFileOutputStream = new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(binaryFile)))
      val logWriter = new PrintWriter(new BufferedOutputStream(new FileOutputStream(textFile)))
      def walk(node:Node){
        if (!previousJointPointsSnapshot.contains(node.id)){
          for (children<-node.children.values){
            walk(children)
          }
          val serializedNode = SerializedNode.newBuilder()
            .setId(uuid2bs(node.id))
            .setIsDir(node.isDir)
            .setName(node.name)
            .setSize(node.size)
            .setImmediateChildrenCount(node.children.size)
          val command = Command.newBuilder().setSerializedNode(serializedNode).build()
          command.writeTo(binaryFileOutputStream)
          logWriter.println(command.getSerializedSize+" bytes: " + command)
        }else{
          met += node.id
          val jp = JointPoint.newBuilder().setId(uuid2bs(node.id))
          val command = Command.newBuilder().setJointPoint(jp).build()
          command.writeDelimitedTo(binaryFileOutputStream)
          logWriter.println(command.getSerializedSize+" bytes: " + command)
        }
      }
      walk(topNode)
      binaryFileOutputStream.close()
      logWriter.close()
      sender ! ChunkMetadata(topNode.id, met.toSet)
      context.stop(self)
    }
  }
}

class TreeSerializationProcessActor extends Actor with XMLUtils{
  var whoToRespond: ActorRef = null
  var totalChunks = 0
  val chunks = mutable.Set[ChunkMetadata]()
  var folder: String = ""

  def runWalker(folder:String, filesystem:FilesystemTree, numberOfParts:Int){
    val jointPoints = mutable.Set[UUID]()

    val totalFiles = filesystem.root.childrenCount
    val chunkLimit = totalFiles/numberOfParts

    def walk(node:Node):Int={
      val total = node.children.values.map(node=>walk(node)).sum + 1
      if (total>chunkLimit){
        val chunkProcessingActor = context.actorOf(Props[ChunkProcessorActor]())
        chunkProcessingActor ! SerializeChunk(folder, node, jointPoints.toSet)
        jointPoints.add(node.id)
        totalChunks += 1
        1
      }else{
        total
      }
    }

    walk(filesystem.root)
  }

  def receive ={
    case msg@StartSerialization(folder, fs, numberOfParts, Some(sender))=> {
      whoToRespond = sender
      this.folder = folder
      runWalker(folder, fs, numberOfParts)
    }
    case msg@ChunkMetadata(provides, depends)=>{
      chunks += msg
      if (chunks.size == totalChunks){
        whoToRespond ! SerializationComplete()
        storeMetadata
        context.stop(self)
      }
    }
  }

  def storeMetadata{
    val xml = <chunks>{
      for (chunk<-chunks) yield {
        <chunk>
          <name>{chunk.provides}</name>
          <depends>{
            for (item<-chunk.depends) yield <item>{item}</item>
          }</depends>
        </chunk>
      }
    }</chunks>
    xml.savePretty(folder+"/meta.xml")
  }
}

class TreeSerializationDispatcherActor extends Actor{
  val processorActors = mutable.Set[ActorRef]()
  val log = Logging(context.system, this)

  def receive = {
    case msg@StartSerialization(folder, fs, numberOfParts, None)=> {
      val processActor = context.actorOf(Props[TreeSerializationProcessActor])
      processorActors.add(processActor)
      processActor ! msg.copy(originalSender = Some(context.self))
    }
    case SerializationComplete() =>{
      println(sender+" completed serialization")
    }
  }
}

trait TreeSerializationComponent extends TreeSerializationComponentApi{ self: AkkaActorsComponentApi=>
  val treeSerializer:TreeSerialization = new TreeSerialization {
    private[this] lazy val serializationDispatcher = actorSystem.actorOf(Props[TreeSerializationDispatcherActor])

    val dir = "./fs/"
    val chunksCount = 8

    def serialize(filesystem: FilesystemTree) {
      serializationDispatcher ! StartSerialization(dir, filesystem, chunksCount)
    }
  }
}
