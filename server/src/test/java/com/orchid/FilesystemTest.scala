package com.orchid

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import com.google.inject.{Guice}
import org.scalatest.{GivenWhenThen, Spec}
import com.orchid.tree.{Node, FilesystemTree, FilesystemTreeModule}
import java.util.UUID
import collection.immutable.HashMap
import util.Random

/**
 * User: Igor Petruk
 * Date: 07.03.12
 * Time: 22:36
 */

@RunWith(classOf[JUnitRunner])
class FilesystemTest extends Spec with GivenWhenThen{

  def fixture = new {
    val injector = Guice.createInjector(new FilesystemTreeModule)
    val filesystem = injector.getInstance(classOf[FilesystemTree])
  }

  describe("A filesystem "){
    it ("should be empty at the beginning"){
      given("empty filesystem")
      val f = fixture
      then("it is empty")
      assert(f.filesystem.root.children.isEmpty === true)
    }
    it ("should not have a non existing file"){
      given("empty filesystem")
      val f = fixture
      when("file is queried")
      val file = f.filesystem.file("ololo/trololo")
      then("it should not be found")
      assert(file === None)
    }
    it ("should return root with empty path"){
      given("empty filesystem")
      val f = fixture
      when("empty path is queried")
      val file = f.filesystem.file("")
      then("it should not be found")
      assert(file === Some(f.filesystem.root))
    }
    it ("should support file creation"){
      given("empty filesystem")
      val f = fixture
      when("file tree is are created")
      val file1 = Node(new UUID(1,1),"file1", true, HashMap());
      val file2 = Node(new UUID(2,2),"file2", true, HashMap());
      val file3 = Node(new UUID(3,3),"file3", true, HashMap());
      val file4 = Node(new UUID(4,4),"file4", true, HashMap());
      val file5 = Node(new UUID(4,4),"file5", true, HashMap());
      f.filesystem.setFile("", file1)
      f.filesystem.setFile(file1.name, file2)
      f.filesystem.setFile(file1.name+"/"+file2.name, file3)
      f.filesystem.setFile(file1.name+"/"+file2.name, file4)
      f.filesystem.setFile(file1.name+"/"+file2.name+"/"+file4.name, file5)

      def assertFile(path:String){
        then(path + " should be found")
        f.filesystem.file(path) match {
          case None=>fail()
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
    val file5 = Node(new UUID(4,4),"file5", true, HashMap());
    var s = "file1"
    val rnd = new Random
    var filesCreated = 0;
    val uuid = new UUID(4,4)
    for(i <- 0 to 200000){
      for (j<-0 to 30){
        val file = Node(uuid,"file"+rnd.nextInt, true, HashMap.empty);
        f.filesystem.setFile(s, file)
        filesCreated+=1
      }
      val start = System.currentTimeMillis()
      f.filesystem.setFile(s, file5)
      val time = System.currentTimeMillis()-start
      filesCreated+=1

      if (i%100==0) println(i+" "+filesCreated+" "+time)
      s+="/"+file5.name
    }
    while(true){}

  }
}
