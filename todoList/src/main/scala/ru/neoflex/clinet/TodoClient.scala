package ru.neoflex.clinet

import cats.effect.{ExitCode, IO, IOApp}
import org.http4s.*
import org.http4s.client.*
import org.http4s.blaze.client.BlazeClientBuilder

import scala.concurrent.ExecutionContext.global
import org.http4s.client.dsl.io.*
import org.http4s.headers.*
import org.http4s.MediaType
import org.http4s.Method.*
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.circe.CirceEntityCodec.circeEntityDecoder
import io.circe.syntax.*
import org.http4s.circe.*
import ru.neoflex.Config
import ru.neoflex.server.TodoItem

//todo: registration in app,
// enter data for load on server,
// UI for select file load/download,
// UI for show user todoItem
object TodoClient extends IOApp with Config:
  def run(args: List[String]): IO[ExitCode] =
    BlazeClientBuilder[IO].resource.use { client =>
      val item = TodoItem("Do some other work")
      val itemApi = Uri.fromString(baseUrl + "/item").getOrElse(???)
      val postTodoItem = POST(
        item,
        itemApi
      )

      for
        status <- client.status(postTodoItem)
        _ <- IO{println(s"Post status: $status")}
        items <- client.expect[List[TodoItem]](baseUrl+"/items")
        _ <- IO{ assert(items.exists(_.text.contains("other"))) }
      yield
        ExitCode.Success
    }