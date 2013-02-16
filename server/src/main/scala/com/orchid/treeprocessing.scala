package com.orchid.treeprocessing

import com.orchid.tree.{Node, FilesystemTree}
import scala.collection.mutable
import java.util.UUID
import scala.concurrent._
import duration.Duration

import ExecutionContext.Implicits.global
import java.io.{PrintWriter, FileOutputStream, BufferedOutputStream, File}
import storage.generated.Storage.{SerializedNode, JointPoint, Command}
import com.google.protobuf.ByteString
import java.nio.ByteBuffer
import java.util.zip.GZIPOutputStream
import com.orchid.actors.AkkaActorsComponentApi
import akka.actor.Actor

trait TreeSerialization{
  def serialize(filesystem:FilesystemTree)
}

trait TreeSerializationComponentApi{
  def treeSerializer:TreeSerialization
}

case class ChunkMetadata(provides:UUID, depends:Set[UUID])

case class StartSerialization(folder:String, filesystem:FilesystemTree, numberOfParts:Int)

class TreeSerializationActor extends Actor{
  def receive = {
    case StartSerialization(folder, fs, numberOfParts)=> {

    }
  }
}

trait TreeSerializationComponent extends TreeSerializationComponentApi{ self: AkkaActorsComponentApi=>
  val treeSerializer:TreeSerialization = new TreeSerialization {
    val dir = "./fs/"

    private[this] def uuidToByteBuffer(uuid:UUID)={
      val bb = ByteBuffer
        .allocate(16)
        .putLong(uuid.getMostSignificantBits)
        .putLong(uuid.getLeastSignificantBits)
      bb.rewind()
      bb
    }

    private[this] def submitForSerialization(topNode:Node, previousJointPointsSnapshot:Set[UUID]) = future {
      val met = mutable.Set[UUID]()
      val chunkDir = new File(dir+topNode.id)
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
            .setId(ByteString.copyFrom(uuidToByteBuffer(node.id)))
            .setIsDir(node.isDir)
            .setName(node.name)
            .setSize(node.size)
            .setImmediateChildrenCount(node.children.size)
          val command = Command.newBuilder().setSerializedNode(serializedNode).build()
          command.writeTo(binaryFileOutputStream)
          logWriter.println(command.getSerializedSize+" bytes: " + command)
        }else{
          met += node.id
          val jp = JointPoint.newBuilder().setId(ByteString.copyFrom(uuidToByteBuffer(node.id)))
          val command = Command.newBuilder().setJointPoint(jp).build()
          command.writeDelimitedTo(binaryFileOutputStream)
          logWriter.println(command.getSerializedSize+" bytes: " + command)
        }
      }
      walk(topNode)
      binaryFileOutputStream.close()
      logWriter.close()
      ChunkMetadata(topNode.id, met.toSet)
    }

    def serialize(filesystem: FilesystemTree) {
      val jointPoints = new mutable.HashMap[UUID, Future[ChunkMetadata]]()
        // with mutable.SynchronizedMap[UUID, SerializedChunk]

      val totalFiles = filesystem.root.childrenCount
      val chunksCount = 8
      val chunkLimit = totalFiles/chunksCount

      def walk(node:Node):Int={
        val total = node.children.values.map(node=>walk(node)).sum + 1
        if (total>chunkLimit){
          val submittedChunk = submitForSerialization(node, jointPoints.keySet.toSet)
          jointPoints.put(node.id, submittedChunk)
          1
        }else{
          total
        }
      }

      walk(filesystem.root)
      for (serializer<-jointPoints.values){
        val result = Await.result(serializer, Duration.Inf)
        println(result.provides+"->"+result.depends)
      }
    }
  }
}
