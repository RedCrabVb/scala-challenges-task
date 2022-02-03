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

trait TodoListRoutes[F[_]]:
  val dsl = Http4sDsl[F]
  import dsl.*

  def hello(text: String)(using Sync[F]): F[TodoItem] =
    TodoItem(text).pure[F]

  //todo: url load file, url remove file
  def todoListRoutes(using Sync[F]): HttpRoutes[F] =
    HttpRoutes.of[F] {
      case GET -> Root / "item" / name =>
        for
          greeting <- hello(name)
          resp <- Ok(greeting)
        yield
          resp
      case GET -> Root / "items" =>
        for
          items <- Storage.list[F]
          resp <- Ok(items)
        yield
          resp
    }

  //todo: rename method, add authentication
  def todoListModifyRoutes(using Concurrent[F]): HttpRoutes[F] =
    HttpRoutes.of[F] {
      case req @ POST -> Root /"item" =>
        for
          item <- req.as[TodoItem]
          _ <- (println(s"Item: $item")).pure
          _ <- Storage.prepend(item)
          resp <- Ok(item)
        yield
          resp
    }