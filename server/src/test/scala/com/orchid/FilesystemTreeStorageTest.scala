package com.orchid

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{GivenWhenThen, FunSpec}
import com.orchid.tree.{Node, FilesystemTree, FilesystemTreeComponent}
import util.Random
import java.util.UUID
import com.orchid.treeprocessing.TreeSerializationComponent
import java.io.{FileNotFoundException, File}
import com.orchid.actors.AkkaActorsComponent

@RunWith(classOf[JUnitRunner])
class FilesystemTreeStorageTest extends FunSpec with GivenWhenThen{
  class FilesystemTestComponent extends FilesystemTreeComponent

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
        for (i<-0 until (2+rnd.nextInt(3))){
          val dirName="dir"+i
          val node = Node(UUID.randomUUID(), dirName, true, 0, Map())
          filesystem.setFile(nodePath,node)
          generateChildren(nodePath+"/"+dirName, level-1)
        }
      }
    }
    for (i<-0 until 10){
      val topDirName="topDir"+i
      val node = Node(UUID.randomUUID(), topDirName, true, 0, Map())
      filesystem.setFile("/",node)
      generateChildren("/"+topDirName, 2+rnd.nextInt(6))
    }
  }

  def prepareFolder{
    def delete(f:File) {
      if (f.isDirectory) {
        for (c<- f.listFiles)
          delete(c);
      }
      !f.delete()
    }
    val dir = "./fs/";
    val dirFile = new File(dir);
    delete(dirFile)
    dirFile.mkdirs()
  }

  describe("Filesystem with loader/serializer"){
    it ("should be empty at the beginning"){
      Given("generated filesystem")
      val f = fixture
      prepareFolder
      generateTree(f.filesystem)
      println("Generated "+f.filesystem.root.childrenCount)
      f.component.treeSerializer.serialize(f.filesystem)
    }
  }
}
