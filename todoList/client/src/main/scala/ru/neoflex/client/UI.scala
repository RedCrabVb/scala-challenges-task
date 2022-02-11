package ru.neoflex.client

import cats.effect.IO
import ru.neoflex.{Files, Notes, NotesTmp}

import scala.collection.mutable.ListBuffer
import scala.io.StdIn.readLine
import scala.util.Try
import scala.util.hashing.Hashing.Default

object UI {
  def addNote(): IO[Command] = {
    for
      _ <- IO.println("Enter name note")
      name <- IO.readLine
      _ <- IO.println("Enter text")
      text <- IO.readLine
      _ <- IO.println("Enter label note")
      label <- IO.readLine
    yield
      SendNote(name, text, label)
  }

  def editNote(id: Int, note: ru.neoflex.client.NotesAndFile): IO[Command] = {
    def changeThisField(field: String, value: => String): IO[Boolean] = {
      for
        _ <- IO.println("Current data: " + value)
        _ <- IO.println(s"Change $field? y/n (default false)")
        result <- IO {
          Try {
            readLine().toLowerCase
              .replace("y", "true")
              .replace("n", "false").toBoolean
          } getOrElse {
            false
          }
        }
      yield
        result
    }

    def getValue[F](field: String, value: => F, readInput: => F, infoForUser: String = ""): IO[F] = {
      changeThisField(field, value.toString).flatMap {
        result =>
          if (result) {
            for
              _ <- IO.println(s"Enter $field " + infoForUser)
              result <- IO.delay(readInput)
            yield
              result
          } else {
            IO.pure(value)
          }
      }

    }

    for {
      note <- IO {
        note.find(_._1.id == id).getOrElse((Notes(), List[Files]()))._1
      }
      name <- getValue("name", note.name, readLine())
      text <- getValue("text", note.text, readLine())
      label <- getValue("label", note.label, readLine())
      status <- getValue("completed", note.status, readLine().toBoolean,
        "(true/false)")
    } yield {
      EditNote(id, name, text, label, status)
    }
  }

//  def selectOperation(): IO[Command] = {
//        """
//          |1. Send note
//          |2. Show notes
//          |3. Show notes with for filter
//          |4. Edit note
//          |5. Delete note
//          |-6. Loading file-
//          |-7. Delete file-
//          |8. Upload file
//          |9. Exit
//          |""".stripMargin)
//      command <- IO.readLine

//        case "8" => { for {
//            _ <- IO.println("Enter id note")
//            id <- IO.delay(readLine().toInt)
//            _ <- IO.println("Enter name file")
//            nameFile <- IO.readLine
//            _ <- IO.println("Enter path to file")
//            pathToFile <- IO.readLine
//          } yield {
//            UploadFile(
//              Api.ftpApi(id.toString, nameFile, Cache.user.login),
//              pathToFile,
//              nameFile
//            )
//          }
//        }

//  }

  def printNotes(list: List[(Notes, Option[ru.neoflex.Files])]): String = {
    "\n\n\n\n\n\n\n-----------------\n" + (for ((item, files) <- list) yield {
      s"""id: ${item.id}
         |name: ${item.name}
         |text: ${item.text}
         |label: ${item.label}
         |status: ${item.status}
         |files: ${files.getOrElse(List())}
         |-----------------""".stripMargin
    }).mkString("\n")
  }

}
