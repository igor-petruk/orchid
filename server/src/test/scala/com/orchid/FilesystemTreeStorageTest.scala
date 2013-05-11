package com.orchid

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{GivenWhenThen, FunSpec}
import com.orchid.tree.{Node, FilesystemTree, FilesystemTreeComponent}
import java.util.Random
import java.util.UUID
import com.orchid.treeprocessing.TreeSerializationComponent
import java.io.{FileNotFoundException, File}
import com.orchid.actors.AkkaActorsComponent
import concurrent.{ExecutionContext, Await}
import concurrent.duration.Duration
import ExecutionContext.Implicits.global
import scala.concurrent.duration._
import com.orchid.connection.ConnectionComponent

@RunWith(classOf[JUnitRunner])
class FilesystemTreeStorageTest extends FunSpec with GivenWhenThen{
  class FilesystemTestComponent extends FilesystemTreeComponent with ConnectionComponent

  def fixture = new {
    val component = new FilesystemTestComponent
      with AkkaActorsComponent
      with TreeSerializationComponent
    val filesystem = component.filesystem
  }

  private[this] def generateTree(filesystem:FilesystemTree){
    val rnd = new Random()
    def generateChildren(nodePath:String, level:Int){
      if (level>0){
        for (i<-0 until (2+rnd.nextInt(6))){
          val dirName="dir"+i
          val node = Node(UUID.randomUUID(), dirName, true, 0, Map())
          filesystem.setFile(nodePath,node)
          generateChildren(nodePath+"/"+dirName, level-1)
        }
      }
    }
    for (i<-0 until 10){
      val topDirName="topDir"+i
      println(topDirName)
      val node = Node(UUID.randomUUID(), topDirName, true, 0, Map())
      filesystem.setFile("/",node)
      generateChildren("/"+topDirName, 4+rnd.nextInt(5))
    }
  }

  def prepareFolder(dir:String){
    def delete(f:File) {
      if (f.isDirectory) {
        for (c<- f.listFiles)
          delete(c);
      }
      !f.delete()
    }
    val dirFile = new File(dir);
    delete(dirFile)
    dirFile.mkdirs()
  }

  describe("Filesystem with loader/serializer"){
    it ("should be empty at the beginning"){
      Given("generated filesystem")
      val f = fixture
      generateTree(f.filesystem)
      println("Generated "+f.filesystem.root.childrenCount)


      def testFor(chunkCount:Int){
        prepareFolder("./fs")
        val start = System.currentTimeMillis()
        val result = f.component.treeSerializer.serialize(f.filesystem,chunkCount)
        result.onSuccess{
          case _ => println("Completed %d chunks for %d ms".format(chunkCount, System.currentTimeMillis()-start))
        }
        Await.ready(result, Duration.Inf)

        val dStart = System.currentTimeMillis()
        val treeResult = f.component.treeDeserializer.deserialize
        treeResult.onSuccess{
          case _ => println("Deserialization completed %d chunks for %d ms".format(chunkCount, System.currentTimeMillis()-dStart))
        }
        Await.ready(treeResult, Duration.Inf)

      }
      for (i<-1 to 4; chunks<-List(1,4,8,16,32,128)){
        testFor(chunks)
      }
    }
  }
}
