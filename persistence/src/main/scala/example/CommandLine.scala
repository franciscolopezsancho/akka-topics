package example.persistence

import scala.util.parsing.combinator.RegexParsers

object CommandLine {

  sealed trait Command

  object Command {

    case class AddCargo(
        containerId: String,
        cargoId: String,
        cargoKind: String,
        cargoSize: Int)
        extends Command
    case object Quit extends Command
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

    def createCargo: Parser[Command.AddCargo] =
      (containerId ~ cargoId ~ cargoKind ~ cargoSize) ^^ {
        case (containerId ~ cargoId ~ cargoKind ~ cargoSize) =>
          Command.AddCargo(containerId, cargoId, cargoKind, cargoSize)
      }

    def quit: Parser[Command.Quit.type] =
      (("quit|exit")) ^^ {
        case _ => Command.Quit
      }

    def stringType: Parser[String] =
      """\w+""".r ^^ (_.toString)

    def containerId: Parser[String] = stringType

    def cargoId: Parser[String] = stringType

    def cargoKind: Parser[String] = stringType

    def cargoSize: Parser[Int] =
      """\d+""".r ^^ (_.toInt)

    val parser: CommandParser.Parser[Command] =
      CommandParser.createCargo | CommandParser.quit
  }
}
