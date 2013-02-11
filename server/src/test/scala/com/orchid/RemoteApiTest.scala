package com.orchid

import com.orchid.tree.Node
import messages.generated.Messages.ErrorType
import org.junit.{Test, After, Before}
import org.junit.Assert._
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSpec, fixture, GivenWhenThen, Spec}
import org.junit.runner.RunWith
import com.orchid.test.{ServerFixtureSupport, TestFilesystemException, TestApi}

/**
 * User: Igor Petruk
 * Date: 15.03.12
 * Time: 22:56
 */

@RunWith(classOf[JUnitRunner])
class RemoteApiTest extends FunSpec with GivenWhenThen with ServerFixtureSupport{

  def fixture = new {
    startMemoryOnlyServer
    val api = new TestApi("localhost",19800)
  }

  describe("Remote API interaction"){
    val api = fixture.api
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
  }
  
}
