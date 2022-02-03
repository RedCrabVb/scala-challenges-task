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

trait TodoListRoutes[F[_]]:
  val dsl: Http4sDsl[F] = Http4sDsl[F]
  import dsl.*

  //todo: url load file, url remove file
  def todoListRoutes(using Sync[F]): HttpRoutes[F] =
    HttpRoutes.of[F] {
      case GET -> Root / "items" =>
        for
          items <- Storage.getAllItems[F] //todo: prohibition of receiving other peoples records
          resp <- Ok(items)
        yield
          resp
    }

  //todo: rename method, add authentication
  def todoListModifyRoutes(using Concurrent[F]): HttpRoutes[F] =
    HttpRoutes.of[F] {
      case req @ POST -> Root / "item" =>
        for
          item <- req.as[TodoItemTmp]
          newItem <- Storage.prependItems(item)
          _ <- println(s"Item: $newItem").pure
          resp <- Ok(newItem)
        yield
          resp
      case req @ POST -> Root / "authorization" =>
        for
          user <- req.as[User]
          result <- Storage.authorization(user)
          _ <- (println(s"authorization for $user: $result")).pure
          resp <- Ok(result)
        yield
          resp
      case req @ POST -> Root / "registration" =>
        for
          user <- req.as[User]
          result <- Storage.registration(user)
          _ <- (println(s"registration for $user: $result")).pure
          resp <- Ok(result)
        yield
          resp
    }