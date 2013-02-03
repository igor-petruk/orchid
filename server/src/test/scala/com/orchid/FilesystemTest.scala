package com.orchid

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSpec, GivenWhenThen, Spec}
import java.util.UUID
import collection.immutable.HashMap
import util.Random
import com.orchid.tree.{FilesystemError, FilesystemTreeComponent, Node, FilesystemTree}

/**
 * User: Igor Petruk
 * Date: 07.03.12
 * Time: 22:36
 */

@RunWith(classOf[JUnitRunner])
class FilesystemTest extends FunSpec with GivenWhenThen{

  class FilesystemTestComponent extends FilesystemTreeComponent
  
  def fixture = new {
    val component = new FilesystemTestComponent
    val filesystem = component.filesystem
  }

  describe("A filesystem "){
    it ("should be empty at the beginning"){
      Given("empty filesystem")
      val f = fixture
      Then("it is empty")
      assert(f.filesystem.root.children.isEmpty === true)
    }
    it ("should not have a non existing file"){
      Given("empty filesystem")
      val f = fixture
      When("file is queried")
      val file = f.filesystem.file("ololo/trololo")
      Then("it should not be found")
      assert(file === None)
    }
    it ("should return root with empty path"){
      Given("empty filesystem")
      val f = fixture
      When("empty path is queried")
      val file = f.filesystem.file("")
      Then("it should not be found")
      assert(file === Some(f.filesystem.root))
    }
    it ("should support file creation"){
      Given("empty filesystem")
      val f = fixture
      When("file tree is are created")
      val file1 = Node(new UUID(1,1),"file1", true, 0,HashMap());
      val file2 = Node(new UUID(2,2),"file2", true, 0,HashMap());
      val file3 = Node(new UUID(3,3),"file3", true, 0,HashMap());
      val file4 = Node(new UUID(4,4),"file4", true, 0,HashMap());
      val file5 = Node(new UUID(4,4),"file5", true, 0,HashMap());
      f.filesystem.setFile("", file1)
      f.filesystem.setFile(file1.name, file2)
      f.filesystem.setFile(file1.name+"/"+file2.name, file3)
      f.filesystem.setFile(file1.name+"/"+file2.name, file4)
      f.filesystem.setFile(file1.name+"/"+file2.name+"/"+file4.name, file5)

      def assertFile(path:String){
        Then(path + " should be found")
        f.filesystem.file(path) match {
          case None=>fail(path+ " not found")
          case _=>
        }
      }
      assertFile("file1")
      assertFile("file1/file2")
      assertFile("file1/file2/file3")
      assertFile("file1/file2/file4")
      assertFile("file1/file2/file4/file5/")
    }
  }

  def benchmark{
    val f = fixture
    val file5 = Node(new UUID(4,4),"file5", true, 0,HashMap());
    var s = ""
    val rnd = new Random
    var filesCreated = 0;
    val uuid = new UUID(4,4)
    var lastCreated: Either[FilesystemError,Node] = null
    for(i <- -2 to 100){
      val start = System.currentTimeMillis()
      val times = 500;
      for (j<-1 until times){
        val file = Node(uuid,"file"+rnd.nextInt, true, 0, HashMap.empty);
        lastCreated=f.filesystem.setFile(s, file)
//        println(s+"->"+file+":"+f.filesystem.setFile(s, file))
        filesCreated+=1
      }
      f.filesystem.setFile(s, file5)
//      println(s+"->"+file5+":"+f.filesystem.setFile(s, file5))
      filesCreated+=1
      val time:Double = System.currentTimeMillis()-start
      if (i%10==0)println(filesCreated+" "+i+" "+times/time*1000)
      s+="/"+file5.name
    }
    println(lastCreated)
    s = ""

    for(i <- -2 to 200000){
      val start = System.currentTimeMillis()
      val times = 500;
      for (j<-1 until times){
        val file = Node(uuid,"file"+rnd.nextInt, true,0, HashMap.empty);
        lastCreated=f.filesystem.setFile(s, file)
        filesCreated+=1
      }
      lastCreated=f.filesystem.setFile(s, file5)
      filesCreated+=1
      val time:Double = System.currentTimeMillis()-start
      if (i%10==0){
        println(filesCreated+" "+i+" "+times/time*1000+" "+lastCreated)
      }
      s+="/"+file5.name
    }
  }
}
