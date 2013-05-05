package com.orchid.treeprocessing

import com.orchid.tree.{Node, FilesystemTree}
import scala.collection.mutable
import java.util.UUID

import java.io._
import storage.generated.Storage.{SerializedNode, JointPoint, Command}
import java.util.zip.{GZIPInputStream, GZIPOutputStream}
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
import xml.{Source, XML}
import annotation.tailrec
import akka.util
import java.util.concurrent.atomic.AtomicInteger
import com.fasterxml.uuid.impl.UUIDUtil
import com.google.protobuf.AbstractMessageLite.Builder.LimitedInputStream
import com.google.common.io.LimitInputStream
import com.google.protobuf.{InvalidProtocolBufferException, CodedOutputStream, CodedInputStream}

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
  implicit val timeout = new util.Timeout(5 minutes)

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
    private[this] lazy val serializationDispatcher = actorSystem.actorOf(Props[TreeDeserializationDispatcherActor])

    def deserialize = (serializationDispatcher ? DeserializeTree(dir)).map {
      case DeserializedChunk(id,node) => node
      case _ => ???
    }
  }
}

abstract sealed trait DeserializationMessage
case class DeserializeTree(folder:String, originalSender: Option[ActorRef] = None)
case class DeserializeChunk(folder:String, metadata:ChunkMetadata, thenSendTo: Set[ActorRef])
case class DeserializedChunk(id:UUID, node:Node)

class TreeDeserializationDispatcherActor extends Actor{
  val processorActors = mutable.Map[ActorRef, ActorRef]()
  val log = Logging(context.system, this)

  def receive = {
    case msg@DeserializeTree(folder, None)=> {
      val processActor = context.actorOf(Props[TreeDeserializationProcessActor])
      processorActors.put(processActor, context.sender)
      processActor ! msg.copy(originalSender = Some(sender))
    }
  }
}

class TreeDeserializationProcessActor extends Actor{
  def receive = {
    case msg@DeserializeTree(folder, Some(originalSender))=>{
      val root = XML.load(Source.fromFile(folder+"/meta.xml"))

      val metadata = for (chunk <- root \ "chunk") yield {
        val uuid = UUID.fromString((chunk \ "name").text)
        val depends = for (dep <- chunk\"depends"\"item") yield UUID.fromString(dep.text)
        ChunkMetadata(uuid, depends.toSet)
      }

      val metadataMap = metadata
        .groupBy(_.provides)
        .mapValues(_.head)
      val actors = metadataMap.map(mi=>(mi._1,context.actorOf(Props[ChunkDeserializingActor])))
      val inverseDependency = metadataMap
        .map{ item => (item._1,metadata
            .filter(_.depends.contains(item._1)))
        }
        .mapValues(_.map(_.provides).toSet)

      val toWhoSend = inverseDependency.mapValues(_.map(actors(_)))  +
        (new UUID(0,0)->Set(originalSender))

      for ((id, actor)<-actors){
        val chunkMetadata = metadataMap(id)
        actor ! DeserializeChunk(folder, chunkMetadata, toWhoSend(id))
      }
    }
  }
}

class ChunkDeserializingActor extends Actor with UUIDConversions{
  var folder:String = null
  var receivedChunks = mutable.Map[UUID, Node]()
  var chunkMetadata: ChunkMetadata = null
  var toWhoSend: Set[ActorRef] = null
  var configured = false

  def deserialize{
    val chunkDir = new File(folder+chunkMetadata.provides)
    val binaryFile = chunkDir.getAbsolutePath+File.separatorChar+"bin"

    val inputStream = new BufferedInputStream(new FileInputStream(binaryFile))
    val codedInputStream = CodedInputStream.newInstance(inputStream)
    val uuidBuffer = Array.ofDim[Byte](16)
    var oldLimit = -1

    val stack = mutable.ArrayStack[Node]()
//    val children = mutable.ArrayBuffer[Node]()

    def limitStreamToSize(size:Int){
      if (oldLimit != -1)
        codedInputStream.popLimit(oldLimit)
      oldLimit = codedInputStream.pushLimit(size)
    }

    @tailrec
    def runNextCommand():Node={
      require(folder!=null)
      require(chunkMetadata!=null)
      require(toWhoSend!=null)
      val command = try{
        limitStreamToSize(4)
        val size = codedInputStream.readFixed32()
        limitStreamToSize(size)
        Command.parseFrom(codedInputStream)
      }catch{
        case e:InvalidProtocolBufferException => null
      }

      if (command!=null){
        if (command.hasSerializedNode){
          val serizalizedNodeCommand = command.getSerializedNode
          var childrenCount = serizalizedNodeCommand.getImmediateChildrenCount

          serizalizedNodeCommand.getId.copyTo(uuidBuffer, 0)
          var childrenMap = Map[String, Node]()

          while (childrenCount>0){
            val child = stack.pop
            childrenMap = childrenMap.updated(child.name, child)
            childrenCount -= 1
          }

          val node = Node(
            UUIDUtil.uuid(uuidBuffer),
            serizalizedNodeCommand.getName,
            serizalizedNodeCommand.getIsDir,
            serizalizedNodeCommand.getSize,
            childrenMap
          )
          stack.push(node)
          runNextCommand()
        }else{
          val jointPointCommand = command.getJointPoint
          val id = bs2uuid(jointPointCommand.getId)
          val dependency = receivedChunks(id)
          stack.push(dependency)
          runNextCommand()
        }
      }else{
        require(stack.size==1)
        stack.head
      }
    }

    val node = runNextCommand()
    require(node.id==chunkMetadata.provides)
    for (actor<-toWhoSend){
      actor ! DeserializedChunk(node.id, node)
    }
    inputStream.close
  }

  def ready = {
    configured && chunkMetadata
      .depends
      .forall(dependency=>
      receivedChunks
        .keys
        .toSet
        .contains(dependency)
    )
  }

  def receive = {
    case msg @ DeserializeChunk(folder, chunkMetadata, toWhoSend)=>{
      this.folder = folder
      this.chunkMetadata = chunkMetadata
      this.toWhoSend = toWhoSend
      configured = true
      if (ready){
        deserialize
      }
    }
    case DeserializedChunk(id, node)=>{
      receivedChunks += (id->node)
      if (ready){
        deserialize
      }
    }
  }
}

abstract sealed trait SerializationMessage
case class ChunkMetadata(provides:UUID, depends:Set[UUID]) extends SerializationMessage
case class StartSerialization(folder:String, filesystem:FilesystemTree, numberOfParts:Int,
         originalSender: Option[ActorRef] = None) extends SerializationMessage
case class SerializeChunk(folder:String,topNode:Node, previousJointPointsSnapshot:Set[UUID]) extends SerializationMessage
case class SerializationComplete() extends SerializationMessage

class ChunkSerializingActor extends Actor with UUIDConversions{

  def receive={
    case SerializeChunk(folder, topNode, previousJointPointsSnapshot)=>{
      val met = mutable.Set[UUID]()
      val chunkDir = new File(folder+topNode.id)
      chunkDir.mkdirs()
      val binaryFile = chunkDir.getAbsolutePath+File.separatorChar+"bin"
      //val textFile = chunkDir.getAbsolutePath+File.separatorChar+"log.txt"
//      val binaryFileOutputStream = new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(binaryFile)))
      val binaryFileOutputStream = new BufferedOutputStream(new FileOutputStream(binaryFile))
      val codedOutputStream = CodedOutputStream.newInstance(binaryFileOutputStream)
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
          codedOutputStream.writeFixed32NoTag( command.getSerializedSize)
          command.writeTo(codedOutputStream)
        //  logWriter.println(command.getSerializedSize+" bytes: " + command)
        }else{
          met += node.id
          val jp = JointPoint.newBuilder().setId(uuid2bs(node.id))
          val command = Command.newBuilder().setJointPoint(jp).build()
          codedOutputStream.writeFixed32NoTag( command.getSerializedSize)
          command.writeTo(codedOutputStream)
        //  logWriter.println(command.getSerializedSize+" bytes: " + command)
        }
      }
      walk(topNode)
      codedOutputStream.flush()
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
      if (total>chunkLimit || node.id == filesystem.root.id){
        val chunkProcessingActor = context.actorOf(Props[ChunkSerializingActor]())
        chunkProcessingActor ! SerializeChunk(folder, node, jointPoints.toSet)
        jointPoints.add(node.id)
        totalChunks += 1
        1
      }else{
        total
      }
    }
    if (numberOfParts==1){
      val chunkProcessingActor = context.actorOf(Props[ChunkSerializingActor]())
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
    println(chunks.map(_.provides))
    chunks.find(_.provides.equals(new UUID(0,0))) match {
      case None=> throw new RuntimeException
      case _ =>
    }
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
