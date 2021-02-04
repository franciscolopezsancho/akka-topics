// package aia.faulttolerance

// import java.io.File
// import java.util.UUID
// import akka.actor.typed.scaladsl.{ Behaviors }
// import akka.actor.typed.{
//   ActorRef,
//   ActorSystem,
//   Behavior,
//   PostStop,
//   SupervisorStrategy,
//   Terminated
// }
// import scala.concurrent.duration._
// import language.postfixOps

// object LogProcessingApp extends App {
//   val sources = Vector("file:///source1/", "file:///source2/")
//   val system = ActorSystem("logprocessing")

//   val databaseUrl = "http://mydatabase1"

//   val guardian =
//     ActorSystem(LogProcessingSupervisor(sources, databaseUrl), "name")
// }

// object LogProcessingSupervisor {

//   def name = "file-watcher-supervisor"

//   def apply(sources: Vector[String], databaseUrl: String): Behavior[Nothing] =
//     Behaviors
//       .setup { (context) =>
//         var fileWatchers = sources.map { source =>
//           val dbWriter =
//             context.spawn(DbWriter(databaseUrl), DbWriter.name(databaseUrl))

//           val logProcessor =
//             context.spawn(LogProcessor(dbWriter), LogProcessor.name)

//           val fileWatcher =
//             context.spawn(FileWatcher(source, logProcessor), FileWatcher.name)
//           context.watch(fileWatcher)
//           fileWatcher
//         }
//         Behaviors.empty
//       }
//       .receiveSignal { (context, signal) =>
//         signal match {
//           case Terminated(actorRef) =>
//           // sources.map(println)
//           // if (fileWatchers.contains(actorRef)) {
//           //   fileWatchers = fileWatchers.filterNot(_ == actorRef)
//           //   if (fileWatchers.isEmpty) {
//           //     context.log.info(
//           //       "Shutting down, all file watchers have failed.")
//           //     context.system.terminate()
//           //   }
//           // }

//         }
//       }

// }

// object FileWatcher {

//   sealed trait Command
//   case class NewFile(file: File, timeAdded: Long) extends Command
//   case class SourceAbandoned(uri: String) extends Command

//   def name = s"file-watcher-${UUID.randomUUID.toString}"

//   def apply(
//       source: String,
//       logProcessor: ActorRef[LogProcessor.Command]): Behavior[Command] = {
//     Behaviors.setup { (context) =>
//       Behaviors.receiveMessage[Command] {
//         case NewFile(file, _) =>
//           logProcessor ! LogProcessor.LogFile(file)
//           Behaviors.same
//         case SourceAbandoned(uri) if uri == source =>
//           Behaviors.stopped

//       }
//     }
//   }
// }

// object LogProcessor extends LogParsing {
//   // with LogParsing {

//   def name = s"log_processor_${UUID.randomUUID.toString}"
//   sealed trait Command
//   case class LogFile(file: File) extends Command

//   def apply(dbWriter: ActorRef[DbWriter.Command]): Behavior[Command] =
//     Behaviors
//       .supervise {
//         Behaviors.receiveMessage[Command] {
//           case LogFile(file) =>
//             val lines: Vector[DbWriter.Line] = parse(file)
//             lines.foreach(dbWriter ! _)
//             Behaviors.same
//         }
//       }
//       .onFailure[CorruptedFileException](SupervisorStrategy.resume)
// }

// object DbWriter {

//   sealed trait Command
//   case class Line(time: Long, message: String, messageType: String)
//       extends Command

//   def name(databaseUrl: String) =
//     s"""db-writer-${databaseUrl.split("/").last}"""

//   def apply(databaseUrl: String): Behavior[Command] =
//     Behaviors
//       .supervise {
//         Behaviors.setup[Command] { context =>
//           val connection = new DbCon(databaseUrl)
//           Behaviors
//             .receiveMessage[Command] {
//               case Line(time, message, messageType) =>
//                 connection.write(
//                   Map(
//                     'time -> time,
//                     'message -> message,
//                     'messageType -> messageType))
//                 Behaviors.same
//             }
//             .receiveSignal {
//               case (_, PostStop) =>
//                 connection.close
//                 Behaviors.same
//             }
//         }
//       }
//       .onFailure[DbBrokenConnectionException](SupervisorStrategy.restart)

// }

// class DbCon(url: String) {

//   /**
//    * Writes a map to a database.
//    * @param map the map to write to the database.
//    * @throws DbBrokenConnectionException when the connection is broken. It might be back later
//    * @throws DbNodeDownException when the database Node has been removed from the database cluster. It will never work again.
//    */
//   def write(map: Map[Symbol, Any]): Unit = {
//     //
//   }

//   def close(): Unit = {
//     //
//   }
// }

// @SerialVersionUID(1L)
// class DiskError(msg: String) extends Error(msg) with Serializable

// @SerialVersionUID(1L)
// class CorruptedFileException(msg: String, val file: File)
//     extends Exception(msg)
//     with Serializable

// @SerialVersionUID(1L)
// class DbBrokenConnectionException(msg: String)
//     extends Exception(msg)
//     with Serializable

// @SerialVersionUID(1L)
// class DbNodeDownException(msg: String) extends Exception(msg) with Serializable

// trait LogParsing {
//   import DbWriter._
//   // Parses log files. creates line objects from the lines in the log file.
//   // If the file is corrupt a CorruptedFileException is thrown
//   def parse(file: File): Vector[Line] = {
//     // implement parser here, now just return dummy value
//     Vector.empty[Line]
//   }
// }
