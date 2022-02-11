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

//fixme: I would like to receive errors from api
trait NotesRoutes:
  val dsl: Http4sDsl[IO] = Http4sDsl[IO]
  import dsl.*

  def itemsRoutes: HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case req @ GET -> Root / "itemShow" =>
        for
          accountTmp <- req.as[Account]
          user <- Storage.authorization(accountTmp)
          items <- Storage.getAllItems(user)
          resp <- Ok(items)
        yield
          resp
      case req @ POST -> Root / "item" =>
        for
          notes <- req.as[(Account, NotesTmp)]
          newItem <- Storage.prependNotes(notes._1, notes._2)
          _ <- IO.println(s"Item add: $newItem")
          resp <- Ok(newItem)
        yield
          resp
      case req @ GET -> Root / "item" / "filter" / filter / value =>
        for
          user <- req.as[Account]
          items <- Storage.getNotesWithLabel(user, filter match {
            case "label" => (item: Notes) => item.label == value
            case "status" => (item: Notes) => item.status == value.toBoolean
          })
          resp <- Ok(items)
        yield
          resp
      case req @ GET -> Root / "item" / "sort" / sort =>
        for
          user <- req.as[Account]
          items <- Storage.sortItems(sort match {
            case "text" => (item: Notes) => item.text
            case "name" => (item: Notes) => item.name
            case _ => ???
          }, null)
          resp <- Ok(items)
        yield
          resp
      case req @ POST -> Root / "item" / "edit" / id =>
        for
          item <- req.as[NotesTmp]
          _ <- Storage.editItems(item, id.toString.toInt)
          _ <- IO.println(s"Edit item id: $id")
          resp <- Ok(id)
        yield
          resp
      case req @ POST -> Root / "item" / "delete" / id =>
        for
          account <- req.as[Account]
          newItem <- Storage.deleteNotes(account, id.toInt)
          _ <- IO.println(s"Delete item: $id")
          resp <- Ok(newItem)
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