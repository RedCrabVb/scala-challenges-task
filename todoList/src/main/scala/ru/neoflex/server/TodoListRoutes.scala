package ru.neoflex.server

import cats.effect.{Concurrent, IO, Sync}
import cats.syntax.all.*
import cats.{Applicative, Monad, MonadThrow}
import io.circe.Encoder
import ru.neoflex.server.TodoItem
import org.http4s.circe.*
import org.http4s.circe.CirceEntityCodec.{circeEntityDecoder, circeEntityEncoder}
import org.http4s.dsl.Http4sDsl
import org.http4s.{EntityEncoder, HttpRoutes}
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.parser._
import ru.neoflex.server.User

import java.util.Date

// todo:
// set status note
// set label
// edit note
trait TodoListRoutes[F[_]]:
  val dsl: Http4sDsl[F] = Http4sDsl[F]
  import dsl.*

  def itemsRoutes(using Concurrent[F]): HttpRoutes[F] =
    HttpRoutes.of[F] {
      case req @ GET -> Root / "itemShow" =>
        for
          user <- req.as[User]
          items <- Storage.getAllItems[F](user)
          resp <- Ok(items)
        yield
          resp
      case req @ POST -> Root / "item" =>
        for
          item <- req.as[TodoItemTmp]
          newItem <- Storage.prependItems(item)
          _ <- println(s"Item: $newItem").pure
          resp <- Ok(newItem)
        yield
          resp
      case req @ GET -> Root / "item" / "filter" / filter / value => {
        for
          user <- req.as[User]
          items <- Storage.getItemsWithLabel[F](user, filter match {
            case "label" => (item: TodoItem) => item.label == value
            case "status" => (item: TodoItem) => item.status == value.toBoolean
          })
          resp <- Ok(items)
        yield
          resp
      }
      case req @ GET -> Root / "item" / "sort" / sort => {
        for
          user <- req.as[User]
          items <- Storage.sortItems[F](sort match {
            case "text" => (item: TodoItem) => item.text
            case "name" => (item: TodoItem) => item.name
            case _ => ???
          }, user.getSession)
          resp <- Ok(items)
        yield
          resp
      }
      case req @ POST -> Root / "item" / "edit" / id =>
        for
          item <- req.as[TodoItemTmp]
          newItem <- Storage.editItems(item, id.toString.toInt)
          _ <- println(s"Edit item: $newItem").pure
          resp <- Ok(newItem)
        yield
          resp
    }

  def authorizationRoutes(using Concurrent[F]): HttpRoutes[F] =
    HttpRoutes.of[F] {

      case req @ POST -> Root / "authorization" =>
        for
          user <- req.as[User]
          result <- Storage.authorization(user)
          _ <- println(s"authorization for $user").pure
          resp <- Ok(user)
        yield
          resp
      case req @ POST -> Root / "registration" =>
        for
          user <- req.as[User]
          result <- Storage.registration(user)
          _ <- println(s"registration for $user, session: ${user.getSession}").pure
          resp <- Ok(user)
        yield
          resp
    }