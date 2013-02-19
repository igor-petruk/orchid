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
import akka.pattern.ask
import concurrent.{ExecutionContext, Future}
import akka.util.Timeout

import scala.concurrent.duration._

import ExecutionContext.Implicits.global

trait TreeSerialization{
  def serialize(filesystem:FilesystemTree, chunksCount:Int):Future[SerializationComplete]
}

trait TreeSerializationComponentApi{
  def treeSerializer:TreeSerialization
}

trait TreeDeserialization{
  def deserialize: Future[Node]
}

trait TreeDeserializationComponentApi{
  def treeDeserializer:TreeDeserialization
}

trait TreeSerializationComponent
  extends TreeSerializationComponentApi
  with TreeDeserializationComponentApi
{ self: AkkaActorsComponentApi=>
  val dir = "./fs/"
  implicit val timeout = new Timeout(5 minutes)

  val treeSerializer:TreeSerialization = new TreeSerialization {
    private[this] lazy val serializationDispatcher = actorSystem.actorOf(Props[TreeSerializationDispatcherActor])

    def serialize(filesystem: FilesystemTree, chunksCount:Int):Future[SerializationComplete] = {
      serializationDispatcher ? StartSerialization(dir, filesystem, chunksCount)map{
        case s: SerializationComplete => s
        case _ => ???
      }
    }
  }

  val treeDeserializer:TreeDeserialization = new TreeDeserialization {
    private[this] lazy val serializationDispatcher = actorSystem.actorOf(Props[TreeSerializationDispatcherActor])

    def deserialize = (serializationDispatcher ? DeserializeTree(dir)).map {
      case n:Node => n
      case _ => ???
    }
  }
}

abstract sealed trait DeserializationMessage
case class DeserializeTree(folder:String, originalSender: Option[ActorRef] = None)

abstract sealed trait SerializationMessage
case class ChunkMetadata(provides:UUID, depends:Set[UUID]) extends SerializationMessage
case class StartSerialization(folder:String, filesystem:FilesystemTree, numberOfParts:Int,
         originalSender: Option[ActorRef] = None) extends SerializationMessage
case class SerializeChunk(folder:String,topNode:Node, previousJointPointsSnapshot:Set[UUID]) extends SerializationMessage
case class SerializationComplete() extends SerializationMessage

class ChunkProcessorActor extends Actor with UUIDConversions{

  def receive={
    case SerializeChunk(folder, topNode, previousJointPointsSnapshot)=>{
      val met = mutable.Set[UUID]()
      val chunkDir = new File(folder+topNode.id)
      chunkDir.mkdirs()
      val binaryFile = chunkDir.getAbsolutePath+File.separatorChar+"bin"
      //val textFile = chunkDir.getAbsolutePath+File.separatorChar+"log.txt"
      val binaryFileOutputStream = new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(binaryFile)))
      //val logWriter = new PrintWriter(new BufferedOutputStream(new FileOutputStream(textFile)))
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
        //  logWriter.println(command.getSerializedSize+" bytes: " + command)
        }else{
          met += node.id
          val jp = JointPoint.newBuilder().setId(uuid2bs(node.id))
          val command = Command.newBuilder().setJointPoint(jp).build()
          command.writeDelimitedTo(binaryFileOutputStream)
        //  logWriter.println(command.getSerializedSize+" bytes: " + command)
        }
      }
      walk(topNode)
      binaryFileOutputStream.close()
      //logWriter.close()
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
    if (numberOfParts==1){
      val chunkProcessingActor = context.actorOf(Props[ChunkProcessorActor]())
      chunkProcessingActor ! SerializeChunk(folder, filesystem.root, Set())
      totalChunks = 1
    }else{
      walk(filesystem.root)
    }
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
        storeMetadata
        whoToRespond ! SerializationComplete()
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
  val processorActors = mutable.Map[ActorRef, ActorRef]()
  val log = Logging(context.system, this)

  def receive = {
    case msg@StartSerialization(folder, fs, numberOfParts, None)=> {
      val processActor = context.actorOf(Props[TreeSerializationProcessActor])
      processorActors.put(processActor, context.sender)
      processActor ! msg.copy(originalSender = Some(context.self))
    }
    case msg:SerializationComplete =>{
      val originalRequester = processorActors(sender)
      processorActors.remove(sender)
      originalRequester ! msg
    }
  }
}
