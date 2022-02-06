package ru.neoflex.clinet

import cats.effect.{ExitCode, IO, IOApp, Sync}
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
import ru.neoflex.Config
import ru.neoflex.server.{TodoItem, TodoItemTmp, User}
import io.circe.generic.auto.*
import io.circe.syntax.*
import cats.effect.unsafe.implicits.global
import ru.neoflex.clinet.TodoClient.user
import scala.util.control.Breaks._

import scala.io.StdIn.readLine

sealed class Command

final case class SendNote(name: String, text: String, label: String) extends Command

final case class ShowNote() extends Command

final case class EditNote(id: Int, name: String, text: String, label: String, status: Boolean) extends Command

final case class RemoveFile() extends Command

final case class UploadFile() extends Command

final case class Authorization(login: String, password: String) extends Command

final case class Registration(login: String, password: String) extends Command

final case class Exit() extends Command


//todo:
// enter data for load on server,
// UI for select file load/download,
// set status note
object TodoClient extends IOApp with Config :
  var user: User = null
  var notes: List[TodoItem] = null

  def run(args: List[String]): IO[ExitCode] =
    BlazeClientBuilder[IO].resource.use { client =>
      UI.start()
      while (user == null) {
        val command = UI.authorization()
        //fixme: code duplication
        val post = command match {
          case Registration(login, password) =>
            val user = User(login, password)
            val registrationApi = Uri.fromString(baseUrl + "/registration").getOrElse(???)
            val postTodoItem = POST(
              user,
              registrationApi
            )
            (postTodoItem, user)
          case Authorization(login, password) =>
            val user = User(login, password)
            val authorizationApi = Uri.fromString(baseUrl + "/authorization").getOrElse(???)
            val postTodoItem = POST(
              user,
              authorizationApi
            )
            (postTodoItem, user)
        }

        if (client.successful(post._1).unsafeRunSync() == true) {
          user = post._2
        }
      }


      breakable {
        while (true) {
          val command = UI.selectOperation()
          val request: IO[ExitCode] = command match {
            case SendNote(name, text, label) =>
              val userSession = user.getSession
              val item = TodoItemTmp(name, text, label, false, userSession)
              val itemApiAdd = Uri.fromString(baseUrl + "/item").getOrElse(???)
              println(item.asJson)
              val postTodoItem = POST(
                item,
                itemApiAdd
              )
              for
                status <- client.status(postTodoItem)
                _ <- IO.println(s"Status: $status")
              yield
                ExitCode.Success
            case ShowNote() => {
              val itemApiShow = Uri.fromString(baseUrl + "/itemShow").getOrElse(???)
              val postTodoItems = GET(
                user,
                itemApiShow
              )
              for {
                list <- client.expect[List[TodoItem]](postTodoItems)
                _ <- IO {notes = list}
                _ <- IO.println(list)
              } yield ExitCode.Success
            }
            case EditNote(id, name, text, label, status) =>
              val updateNote = TodoItemTmp(name, text, label, status, user.getSession)
              val itemApiEdit = Uri.fromString(baseUrl + "/item/edit/" + id).getOrElse(???)

              val editNote = POST(
                updateNote,
                itemApiEdit
              )
              for
                status <- client.status(editNote)
                _ <- IO.println(s"Status: $status")
              yield
                ExitCode.Success
            case Exit() => break()
            case _ => ???
          }

          request.unsafeRunSync()
        }
      }
      IO(ExitCode.Success)
    }
