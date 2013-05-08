package com.orchid

import com.orchid.tree.Node
import messages.generated.Messages.ErrorType
import org.junit.{Test, After, Before}
import org.junit.Assert._
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSpec, fixture, GivenWhenThen, Spec}
import org.junit.runner.RunWith
import com.orchid.test.{EnvironmentVariableSettings, ServerFixtureSupport, TestFilesystemException, TestApi}
import com.orchid.tree._
import java.util.UUID

/**
 * User: Igor Petruk
 * Date: 15.03.12
 * Time: 22:56
 */

@RunWith(classOf[JUnitRunner])
class RemoteApiTest extends FunSpec with GivenWhenThen with ServerFixtureSupport with EnvironmentVariableSettings{

  def fixture = new {
    startMemoryOnlyServer
    val api = new TestApi(host,port)
    val api2 = new TestApi(host,port)
    api.introduce(new UUID(1,2),7777)
    api2.introduce(new UUID(6,2),7778)
  }

  describe("Remote API interaction"){
    val f = fixture
    val api = f.api
    val api2 = f.api2
    it ("should allow dir creation"){
      Given("empty filesystem")
      When("dir1 dir is created")
      val dir1 = api.makeDir("dir1")
      Then("it should be created")
      dir1 match {
        case Some(Node(_,"dir1",true,0,_,_))=>
        case _=> fail()
      }
    }
    it ("should allow subdir creation"){
      Given("dir1 dir")
      When("dir1/dir2 dir is created")
      val dir2 = api.makeDir("dir1/dir2")
      Then("it should be created")
      dir2 match {
        case Some(Node(_,"dir1/dir2",true,0,_,_))=>
        case _=> fail()
      }
    }
    it ("should not allow double dir creation"){
      Given("dir1/dir2 dir")
      When("dir1/dir2 dir is created")
      val exception = intercept[TestFilesystemException]{
        api.makeDir("dir1/dir2")
      }
      Then("FILE_EXISTS exception should be thrown")
      exception match {
        case TestFilesystemException(ErrorType.FILE_EXISTS,_)=>
        case _ => fail()
      }
    }
    it ("should not allow dir creation in non-existing dirs"){
      Given("dir1 dir")
      When("dir2/dir1 dir is created")
      val exception = intercept[TestFilesystemException]{
        api.makeDir("dir2/dir1")
      }
      Then("FILE_NOT_FOUND exception should be thrown")
      exception match {
        case TestFilesystemException(ErrorType.FILE_NOT_FOUND,_)=>
        case _ => fail()
      }
    }
    it ("should not allow file creating with discovery"){
      Given("dir1 dir")
      When("dir1/file1 dir is created")
      val id = UUID.randomUUID()
      val result = api.createFile(id,"dir1/file1",200)
      api2.discoverFile(id)
      Then("FILE_NOT_FOUND exception should be thrown")
      val peers = api.getGetFilePeers(id)
      println(peers)
    }
  }
  
}
