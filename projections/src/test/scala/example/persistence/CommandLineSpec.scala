package example.persistence

import org.scalatest._
import flatspec._
import matchers._

import example.persistence.CommandLine.Command

class CommandLineSpec extends AnyFlatSpec with should.Matchers {

  "Calling Command.apply" should 
    "create the correct CreateGuest command for the given input" in {
      Command("destination 123 london true") shouldBe (Command.Destination("123", "london", true))
    }
  
}

