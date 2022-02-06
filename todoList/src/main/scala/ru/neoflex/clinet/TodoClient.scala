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
import ru.neoflex.clinet.TodoClient.{baseUrl, user}

import scala.util.control.Breaks.*
import scala.io.StdIn.readLine
import Api._

sealed class Command

final case class SendNote(name: String, text: String, label: String) extends Command

final case class ShowNote() extends Command

final case class ShowNoteFilter(api: Uri) extends Command

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
  var user: User = _
  var notes: List[TodoItem] = _

  def run(args: List[String]): IO[ExitCode] =
    BlazeClientBuilder[IO].resource.use { client =>
      UI.start()
      while (user == null) {
        val command = UI.authorization()
        //fixme: code duplication
        val post = command match {
          case Registration(login, password) =>
            val user = User(login, password)
            val postTodoItem = POST(
              user,
              registrationApi
            )
            (postTodoItem, user)
          case Authorization(login, password) =>
            val user = User(login, password)

            val postTodoItem = POST(
              user,
              authorizationApi
            )
            (postTodoItem, user)
          case _ => throw new Exception
        }

        if (client.successful(post._1).unsafeRunSync()) {
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
            case ShowNote() =>
              val postTodoItems = GET(
                user,
                itemApiShow
              )
              for {
                list <- client.expect[List[TodoItem]](postTodoItems)
                _ <- IO.delay( { notes = list })
                _ <- IO.println(list)
              } yield ExitCode.Success
            case EditNote(id, name, text, label, status) =>
              val updateNote = TodoItemTmp(name, text, label, status, user.getSession)

              val editNote = POST(
                updateNote,
                itemApiEdit(id)
              )
              for
                status <- client.status(editNote)
                _ <- IO.println(s"Status: $status")
              yield
                ExitCode.Success
            case Exit() => break()
            case ShowNoteFilter(api) => {
              val postTodoItems = GET(
                user,
                api
              )
              for {
                list <- client.expect[List[TodoItem]](postTodoItems)
                _ <- IO.delay( { notes = list })
                _ <- IO.println(list)
              } yield ExitCode.Success
            }
            case _ => ???
          }

          request.unsafeRunSync()
        }
      }
      IO(ExitCode.Success)
    }
