package com.orchid

import org.scalatest.{GivenWhenThen, FunSpec}
import com.orchid.table.{Table, Index}

class TableTest extends FunSpec with GivenWhenThen{
  case class User(name:String, lastName: String, age:Int)

  val nameIndex = Index[User](_.name)
  val lastNameIndex = Index[User](_.lastName)
  val ageIndex = Index[User](_.age.asInstanceOf[Integer])

  val user1 = User("Igor","Petruk",23)
  val user2 = User("Rocksy","Seletska",22)
  val user3 = User("Someone","Else",22)
  val all = Set(user1, user2)

  def fixture={
     Table[User]()
      .withIndex(nameIndex)
  }

  describe("Table collection"){
    it("should be empty at start"){
      Given("empty table")
      val table = fixture
      Then("empty")
      assert(table.isEmpty === true)
    }

    it("should support iteration"){
      Given("When following set is added "+all)
      val table = fixture ++ all
      Then("size should be "+all.size)
      assert(table.size === all.size)
      Then("all items should be there")
      val tableList = table.toSeq
      assert(tableList.contains(user1) === true)
      assert(tableList.contains(user2) === true)
    }

    it("should support index query"){
      Given("When following set is added "+all)
      val table = fixture + user1 + user2
      Then(s"query on nameIndex on ${user1.name} should return Some(Set($user1))")
      assert(table(nameIndex).get(user1.name) === Some(Set(user1)))
      Then(s"query on nameIndex on ${user2.name} should return Some(Set($user2))")
      assert(table(nameIndex).get(user2.name) === Some(Set(user2)))
      Then(s"query on nameIndex on Cookie should return None")
      assert(table(nameIndex).get("Cookie") === None)
    }

    it("should support index creation after db fill up"){
      Given("When following set is added "+all)
      var table = fixture + user1 + user2
      And("after that lastName index added")
      table = table.withIndex(lastNameIndex)
      Then(s"query on nameIndex on ${user1.name} should return Some(Set($user1))")
      assert(table(nameIndex).get(user1.name) === Some(Set(user1)))
      Then(s"query on nameIndex on ${user2.name} should return Some(Set($user2))")
      assert(table(nameIndex).get(user2.name) === Some(Set(user2)))
      Then(s"query on nameIndex on Petruk should return None")
      assert(table(nameIndex).get("Petruk") === None)
      Then(s"query on lastNameIndex on ${user1.lastName} should return Some(Set($user1))")
      assert(table(lastNameIndex).get(user1.lastName) === Some(Set(user1)))
      Then(s"query on lastNameIndex on ${user2.lastName} should return Some(Set($user2))")
      assert(table(lastNameIndex).get(user2.lastName) === Some(Set(user2)))
      Then(s"query on lastNameIndex on Igor should return None")
      assert(table(lastNameIndex).get("Igor") === None)
    }

    it("should support items removal"){
      Given("When following set is added "+(all+user3))
      var table = fixture.withIndex(ageIndex) + user1 + user2 + user3
      And(s"$user2 is removed")
      table -= user2
      Then(s"query on nameIndex on ${user1.name} should return Some(Set($user1))")
      assert(table(nameIndex).get(user1.name) === Some(Set(user1)))
      Then(s"query on nameIndex on ${user2.name} should return None")
      assert(table(nameIndex).get(user2.name) === None)
      Then(s"query on ageIndex on ${user2.age} should return Some(Set($user3))")
      assert(table(ageIndex).get(user2.age.asInstanceOf[Integer]) === Some(Set(user3)))
    }
  }
}
