package ru.neoflex.server

import cats.effect.{Concurrent, IO, Sync}
import cats.syntax.all.*
import cats.{Applicative, Monad, MonadThrow}
import io.circe.Encoder
import org.http4s.circe.*
import org.http4s.circe.CirceEntityCodec.{circeEntityDecoder, circeEntityEncoder}
import org.http4s.dsl.Http4sDsl
import org.http4s.{EntityEncoder, HttpRoutes}
import io.circe.generic.auto.*
import io.circe.syntax.*
import io.circe.parser.*
import ru.neoflex.{Account, Notes, NotesTmp}
import ru.neoflex.fs2.Fs2TransportFile
import ru.neoflex.Account
import ru.neoflex.server.Storage.xa

import java.util.Date

import cats.effect.unsafe.implicits.global

//fixme: I would like to receive errors from api
trait NotesRoutes:
  val dsl: Http4sDsl[IO] = Http4sDsl[IO]
  import dsl.*

  println(Storage.getNotesWithFilter(Account("", "", 43), "label", "test").unsafeRunSync().mkString(", "))


  def notesRoutes: HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case req @ GET -> Root / "note" / "load" =>
        for
          accountTmp <- req.as[Account]
          account <- Storage.authorization(accountTmp)
          notes <- Storage.getAllNotes(account)
          resp <- Ok(notes)
        yield
          resp
      case req @ POST -> Root / "note" / "add" =>
        for
          notes <- req.as[(Account, NotesTmp)]
          account <- Storage.authorization(notes._1)
          newnote <- Storage.prependNotes(account, notes._2)
          _ <- IO.println(s"note add: $newnote")
          resp <- Ok(newnote)
        yield
          resp
      case req @ GET -> Root / "note" / "filter" / filter / value =>
        for
          accountTmp <- req.as[Account]
          account <- Storage.authorization(accountTmp)
          notes <- Storage.getNotesWithFilter(account, filter, value)
          resp <- Ok(notes)
        yield
          resp
      case req @ GET -> Root / "note" / "sort" / sort =>
        for
          accountTmp <- req.as[Account]
          account <- Storage.authorization(accountTmp)
          notes <- Storage.sortNotes(sort, account)
          resp <- Ok(notes)
        yield
          resp
      case req @ POST -> Root / "note" / "edit" / id =>
        for
          note <- req.as[(Account, NotesTmp)]
          _ <- Storage.editNotes(note._1, note._2, id.toString.toInt)
          _ <- IO.println(s"Edit note id: $id")
          resp <- Ok(id)
        yield
          resp
      case req @ POST -> Root / "note" / "delete" / id =>
        for
          accountTmp <- req.as[Account]
          account <- Storage.authorization(accountTmp)
          newnote <- Storage.deleteNotes(account, id.toInt)
          _ <- IO.println(s"Delete note: $id")
          resp <- Ok(newnote)
        yield
          resp
    }

  def authorizationRoutes: HttpRoutes[IO] =
    HttpRoutes.of[IO] {

      case req @ POST -> Root / "authorization" =>
        for
          account <- req.as[Account]
          result <- Storage.authorization(account)
          _ <- IO.println(s"authorization for $result")
          resp <- Ok(result)
        yield
          resp
      case req @ POST -> Root / "registration" =>
        for
          account <- req.as[Account]
          result <- Storage.registration(account)
          _ <- IO.println(s"registration for $account, session: ${result}")
          resp <- Ok(account)
        yield
          resp
    }

  def ftpRoutes: HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case req @ POST -> Root / "ftp" / userName / id / nameFile =>
        for
          account <- req.as[Account]
          port <- IO(Fs2TransportFile.blockPort(nameFile, account))
          _ <- IO(Storage.addFile(id.toInt, nameFile, account))
          _ <- IO.println(s"save file ${account.login} on port: $port")
          resp <- Ok(port)
        yield
          resp
      case req @ POST -> Root / "ftp" / port =>
        for
          user <- req.as[Account]
          _ <- IO{
            Fs2TransportFile.unblockPort(port, user)
          }
          resp <- Ok(port)
        yield
          resp
    }