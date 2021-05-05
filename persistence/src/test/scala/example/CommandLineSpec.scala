package example.persistence

import org.scalatest._
import flatspec._
import matchers._

import example.persistence.CommandLine.Command

class CommandLineSpec extends AnyFlatSpec with should.Matchers {

  "Calling Command.apply" should
  "create the correct AddCargo command for the given input" in {
    Command("a b c 1") shouldBe (Command
      .AddCargo("a", "b", "c", 1))
  }

}
