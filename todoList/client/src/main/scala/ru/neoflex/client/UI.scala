package ru.neoflex.client

import cats.effect.IO
import ru.neoflex.{Files, Notes, NotesTmp}

import scala.collection.mutable.ListBuffer
import scala.io.StdIn.readLine
import scala.util.Try
import scala.util.hashing.Hashing.Default

object UI {
  def addNote(): IO[NotesTmp] = {
    for
      _ <- IO.println("Enter name note")
      name <- IO.readLine
      _ <- IO.println("Enter text")
      text <- IO.readLine
      _ <- IO.println("Enter label note")
      label <- IO.readLine
    yield
      NotesTmp(name, text, label, false)
  }

  def editNote(id: Int, note: ru.neoflex.client.NotesAndFile): IO[NotesTmp] = {
    def changeThisField(field: String, value: => String): IO[Boolean] = {
      for
        _ <- IO.println("Current data: " + value)
        _ <- IO.println(s"Change $field? y/n (default false)")
        result <- IO.delay(readLine().toLowerCase == "y")
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
      note <- IO(note.find(_._1.id == id).getOrElse((Notes(), List[Files]()))._1)
      name <- getValue("name", note.name, readLine())
      text <- getValue("text", note.text, readLine())
      label <- getValue("label", note.label, readLine())
      status <- getValue("completed", note.status, readLine().toBoolean,
        "(true/false)")
    } yield {
      NotesTmp(name, text, label, status)
    }
  }

  def printNotes(listUncompressed: List[(Notes, Option[Files])]): String = {
    val list = listUncompressed.groupMapReduce(_._1.id) {
      case (n, fs) => (n, fs.toList)
    } {
      case ((notes, fs1), (_, fs2)) => (notes, fs2 ::: fs1)
    }.valuesIterator

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
