package ru.neoflex.client

import cats.effect.{ExitCode, IO}
import org.http4s.Method.POST
import org.http4s.{Request, Uri}
import org.http4s.client.Client
import org.http4s.dsl.Http4sDsl
import ru.neoflex.{Account, Config, Files, Notes, NotesTmp}
import org.http4s.headers.*
import org.http4s.Method.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.http4s.{MediaType, Request, client, *}
import org.http4s.client.{Client, *}
import org.http4s.blaze.client.BlazeClientBuilder

import scala.concurrent.ExecutionContext.global
import org.http4s.client.dsl.io.*
import org.http4s.headers.*
import org.http4s.Method.*
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.circe.CirceEntityCodec.circeEntityDecoder
import org.http4s.circe.*
import ru.neoflex.{Account, Notes, NotesTmp}
import io.circe.generic.auto.*
import io.circe.syntax.*

import scala.concurrent.duration.*
import scala.util.control.Breaks.*
import scala.io.StdIn.readLine
import Api.*
import cats.effect.Temporal
import cats.effect.Concurrent
import ru.neoflex.fs2.Fs2TransportFile

sealed trait Command {
  def createRequest(client: Client[IO], account: Account): IO[ExitCode]
}

final case class SendNote(name: String, text: String, label: String) extends Command {
  def createRequest(client: Client[IO], account: Account): IO[ExitCode] = {
    val item = NotesTmp(name, text, label, false)
    val postNotesAdd = POST((account, item), noteApiAdd)
    for
      status <- client.status(postNotesAdd)
      _ <- IO.println(s"Status: $status")
    yield
      ExitCode.Success
  }
}

final case class ShowNote() extends Command {
  def createRequest(client: Client[IO], account: Account): IO[ExitCode] = {
    for
      notes <- client.expect[ru.neoflex.client.NotesAndFile](GET(account, noteApiLoad))
      _ <- IO.println(UI.printNotes(notes))
    yield ExitCode.Success
  }
}

final case class ShowNoteFilter(api: Uri) extends Command {
  def createRequest(client: Client[IO], account: Account): IO[ExitCode] = {
    for
      filterNote <- client.expect[ru.neoflex.client.NotesAndFile](GET(account, api))
      _ <- IO.println(UI.printNotes(filterNote))
    yield ExitCode.Success
  }
}

final case class ShowNoteSort(api: Uri) extends Command {
  def createRequest(client: Client[IO], account: Account): IO[ExitCode] = {
    for
      sortNote <- client.expect[ru.neoflex.client.NotesAndFile](GET(account, api))
      _ <- IO.println(UI.printNotes(sortNote))
    yield ExitCode.Success
  }
}

final case class EditNote(id: Int, name: String, text: String, label: String, status: Boolean) extends Command {
  def createRequest(client: Client[IO], account: Account): IO[ExitCode] = {
    val updateNote = (account, NotesTmp(name, text, label, status))
    val editNote = POST(updateNote, noteApiEdit(id))
    for
      status <- client.status(editNote)
      _ <- IO.println(s"Status: $status")
    yield
      ExitCode.Success
  }
}

final case class Delete(id: Int) extends Command {
  def createRequest(client: Client[IO], account: Account): IO[ExitCode] = {
    for
      _ <- client.status(POST(account, Api.notesApiDelete(id)))
    yield ExitCode.Success
  }
}

final case class RemoveFile() extends Command {
  def createRequest(client: Client[IO], account: Account): IO[ExitCode] = ???
}

final case class UploadFile(api: Uri, pathFile: String) extends Command {
  def createRequest(client: Client[IO], account: Account): IO[ExitCode] = {
    for
      port <- client.expect[String](POST(account, api))
      _ <- IO.println(port)
      _ <- Fs2TransportFile.sendFile[IO](port.toInt, pathFile).compile.drain
      _ <- IO.sleep(10.seconds)
      _ <- client.expect[String](POST(account, Api.ftpApiClose(port)))
    yield ExitCode.Success
  }
}

final case class Authorization(login: String, password: String) extends Command {
  def createRequest(client: Client[IO], account: Account): IO[ExitCode] = ???
}

final case class Registration(login: String, password: String) extends Command {
  def createRequest(client: Client[IO], account: Account): IO[ExitCode] = ???
}

final case class UnitCommand() extends Command {
  def createRequest(client: Client[IO], account: Account): IO[ExitCode] = ???
}

