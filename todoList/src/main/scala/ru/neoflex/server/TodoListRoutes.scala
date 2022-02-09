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
import ru.neoflex.fs2.Fs2TransportFile
import ru.neoflex.server.Account

import java.util.Date

//fixme: I would like to receive errors from api
trait TodoListRoutes[F[_]]:
  val dsl: Http4sDsl[F] = Http4sDsl[F]
  import dsl.*

  def itemsRoutes(using Concurrent[F]): HttpRoutes[F] =
    HttpRoutes.of[F] {
      case req @ GET -> Root / "itemShow" =>
        for
          user <- req.as[Account]
          items <- Storage.getAllItems[F](user)
          resp <- Ok(items)
        yield
          resp
      case req @ POST -> Root / "item" =>
        for
          notes <- req.as[(Account, NotesTmp)]
          newItem <- Storage.prependNotes(notes._1, notes._2)
          _ <- println(s"Item add: $newItem").pure
          resp <- Ok(newItem)
        yield
          resp
      case req @ GET -> Root / "item" / "filter" / filter / value => ???
//        for
//          user <- req.as[Notes]
//          items <- Storage.getNotesWithLabel[F](user, filter match {
//            case "label" => (item: TodoItem) => item.label == value
//            case "status" => (item: TodoItem) => item.status == value.toBoolean
//          })
//          resp <- Ok(items)
//        yield
//          resp
      case req @ GET -> Root / "item" / "sort" / sort => ???
//        for
//          user <- req.as[User]
//          items <- Storage.sortItems[F](sort match {
//            case "text" => (item: TodoItem) => item.text
//            case "name" => (item: TodoItem) => item.name
//            case _ => ???
//          }, user.getSession)
//          resp <- Ok(items)
//        yield
//          resp
      case req @ POST -> Root / "item" / "edit" / id => ???
//        for
//          item <- req.as[TodoItemTmp]
//          newItem <- Storage.editItems(item, id.toString.toInt)
//          _ <- println(s"Edit item: $newItem").pure
//          resp <- Ok(newItem)
//        yield
//          resp
      case req @ POST -> Root / "item" / "delete" / id => ???
//        for
//          user <- req.as[User]
//          newItem <- Storage.deleteTodoItem(user, id.toInt)
//          _ <- println(s"Delete item: $id").pure
//          resp <- Ok(newItem)
//        yield
//          resp
    }

  def authorizationRoutes(using Concurrent[F]): HttpRoutes[F] =
    HttpRoutes.of[F] {

      case req @ POST -> Root / "authorization" =>
        for
          account <- req.as[Account]
          result <- Storage.authorization(account)
          _ <- println(s"authorization for $result").pure
          resp <- Ok(result)
        yield
          resp
      case req @ POST -> Root / "registration" =>
        for
          account <- req.as[Account]
          result <- Storage.registration(account)
          _ <- println(s"registration for $account, session: ${result}").pure
          resp <- Ok(account)
        yield
          resp
    }

  def ftpRoutes(using Concurrent[F]): HttpRoutes[F] =
    HttpRoutes.of[F] {
      case req @ POST -> Root / "ftp" / userName / id / nameFile => ???
//        for
//          account <- req.as[Account]
//          port <- Concurrent[F].pure(Fs2TransportFile.blockPort(nameFile, account))
//          _ <- Concurrent[F].pure(Storage.addFile(id.toInt, nameFile, account))
//          _ <- println(s"save file ${account.getSession} on port: $port").pure
//          resp <- Ok(port)
//        yield
//          resp
      case req @ POST -> Root / "ftp" / port => ???
//        for
//          user <- req.as[User]
//          _ <- Concurrent[F].pure(Fs2TransportFile.unblockPort(port, user))
//          resp <- Ok(port)
//        yield
//          resp
    }