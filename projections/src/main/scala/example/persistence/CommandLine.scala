package example.persistence

import scala.util.parsing.combinator.RegexParsers
import java.time.Instant

object CommandLine {

  sealed trait Command

  object Command {

    case class Destination(
        parcelId: String,
        city: String,
        isFinal: Boolean)
        extends Command
    case class HandOver(parcelId: String, city: String)
        extends Command
    case class Unknown(consoleInput: String) extends Command

    def apply(consoleInput: String): Command =
      CommandParser.parseAsCommand(consoleInput)
  }

  object CommandParser extends RegexParsers {

    def parseAsCommand(s: String): Command =
      parseAll(parser, s) match {
        case Success(command, _) => command
        case _                   => Command.Unknown(s)
      }

    def createDestination: Parser[Command.Destination] =
      ("destination|d".r ~> (parcelId ~ city ~ isFinal )) ^^ {
        case (parcelId ~ city ~ isFinal) =>
          Command.Destination(parcelId, city, false)
      }

    def createHandOver: Parser[Command.HandOver] =
      ("handover|h".r ~> (parcelId ~ city)) ^^ {
        case (parcelId ~ city) =>
          Command.HandOver(parcelId, city)
      }

    def parcelId: Parser[String] =
      """\w+""".r ^^ (_.toString)

    def city: Parser[String] =
      """\w+""".r ^^ (_.toString)

    def isFinal: Parser[Boolean] =
      "true|false".r ^^ (_.toBooleanOption.getOrElse(false))

    val parser: CommandParser.Parser[Command] =
      CommandParser.createDestination | CommandParser.createHandOver
  }
}
