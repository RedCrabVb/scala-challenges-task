package ru.neoflex.clinet

import cats.effect.IO
import ru.neoflex.server.TodoItem

import scala.io.StdIn.readLine
import scala.util.Try
import scala.util.hashing.Hashing.Default

object UI {
  private[this] def sendNote(): IO[Command] = {
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

  private[this] def filterAndSort(): IO[Command] = {
    for {
      _ <- IO.println("Filter and sort select: ")
      _ <- IO.println(
        """
          |1. Filter label
          |2. Sort size text
          |3. Filter status
          |4. Sort on text""".stripMargin)
      select <- IO.readLine
      uri <- select match {
        case "1" =>
          for
            _ <- IO.println("Enter label: ")
            value <- IO.readLine
          yield
            Api.itemApiFilter("label", value)
        case "2" =>
          IO(Api.itemApiSort("text"))
        case "3" =>
          for
            _ <- IO.println("Enter status for filter (true/false)")
            statusForFilter <- IO.readLine
          yield
            Api.itemApiFilter("status", statusForFilter.toLowerCase)
        case "4" =>
          IO(Api.itemApiSort("text"))
        case _ => ???
      }
    } yield {
      ShowNoteFilter(uri)
    }
  }

  private[this] def editNote(): IO[Command] = {
    def changeThisField(field: String, value: => String): IO[Boolean] = {
      for
        _ <- IO.println("Current data: " + value)
        _ <- IO.println(s"Change $field? true/false (default false)")
        result <- IO {
          Try {
            readLine().toLowerCase.toBoolean
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
      _ <- IO.println("Enter id note")
      id <- IO {
        readLine().toInt
      }
      note <- IO {
        Try(Cache.notes.find(_.id == id).head).
          getOrElse(TodoItem(id, "not found", "not found", "not found", false, List(), "not found"))
      }
      name <- getValue("name", note.name, readLine())
      text <- getValue("text", note.text, readLine())
      label <- getValue("label", note.label, readLine())
      status <- getValue("completed", note.status, readLine().toLowerCase.toBoolean, "(true/false)")
    } yield {
      EditNote(id, name, text, label, status)
    }
  }

  def start(): IO[Unit] = {
    IO.println(
      """
        |In the program you can:
        |* send notes
        |* attach to notes big files
        |* authorization and registration
        |""".stripMargin)
  }

  def authorization(): IO[Command] = {
    for {
      _ <- IO.println("Authorization on registration in account")
      _ <- IO.println("Select command: ")
      _ <- IO.println(
        """
          |1. Authorization
          |2. Registration""".stripMargin)
      commandStr <- IO.readLine
      command <- commandStr match {
        case "1" =>
          for {
            _ <- IO.println("Enter your login")
            login <- IO.readLine
            _ <- IO.println("Enter your password")
            password <- IO.readLine
          } yield {
            Authorization(login, password)
          }
        case "2" =>
          for
            _ <- IO.println("Enter your login")
            login <- IO.readLine
            _ <- IO.println("Enter your password")
            password <- IO.readLine
          yield
            Registration(login, password)
      }
    } yield
      command

  }

  def selectOperation(): IO[Command] = {
    for {
      _ <- IO.println("\n\nSelect command: ")
      _ <- IO.println(
        """
          |1. Send note
          |2. Show notes
          |3. Show notes with for filter
          |4. Edit note
          |5. Delete note
          |-6. Remove file
          |7. Upload file
          |8. Exit
          |""".stripMargin)
      command <- IO.readLine
      result <- command match {
        case "1" =>
          sendNote()
        case "2" => IO {
          ShowNote()
        }
        case "3" =>
          filterAndSort()
        case "4" =>
          editNote()
        case "5" =>
          for
            _ <- IO.println("Enter id")
            id <- IO.readLine
          yield
            Delete(id.toInt)
        case "6" => IO.delay(RemoveFile())
        case "7" => { for {
            _ <- IO.println("Enter name file")
            nameFile <- IO.readLine
            _ <- IO.println("Enter id note")
            id <- IO.delay(readLine().toInt)
          } yield {
            UploadFile(
              Api.ftpApi(id.toString, Cache.user.login + "__" + nameFile),
              Cache.notes.find(_.id == id).getOrElse(???).toTodoItemTmp()
            )
          }
        }
        case "8" => IO.delay(Exit())
        case _ => ???
      }
    } yield {
      result
    }
  }

  def printTodoItem(list: List[TodoItem]): String = {
    "\n\n\n\n\n\n\n-----------------\n" + (for (item <- list) yield {
      s"""id: ${item.id}
         |name: ${item.name}
         |text: ${item.text}
         |label: ${item.label}
         |status: ${item.status}
         |files: ${item.files.mkString(", ")}
         |-----------------""".stripMargin
    }).mkString("\n")
  }
}
