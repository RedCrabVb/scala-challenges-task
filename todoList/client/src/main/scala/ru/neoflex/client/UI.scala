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

  def printNotes(listUncompressed: List[(Notes, Option[Files])]): String = {
    var list = List[(Notes, List[Option[Files]])]()//fixme: переписать этот код в адекватном состояние
    listUncompressed.foreach(e => if (!list.exists(_._1.id == e._1.id)) {
      list = (e._1, List(e._2)) :: list
    } else {
      val addE: List[Option[Files]] = list.find(_._1.id == e._1.id).get._2 ++ List(e._2)
      list = (e._1, addE) :: list.filter(_._1.id != e._1.id)
    })

    "\n\n\n\n\n\n\n-----------------\n" + (for ((item, files) <- list) yield {
      s"""id: ${item.id}
         |name: ${item.name}
         |text: ${item.text}
         |label: ${item.label}
         |status: ${item.status}
         |files: ${files.mkString(", ")}
         |-----------------""".stripMargin
    }).mkString("\n")
  }

}
