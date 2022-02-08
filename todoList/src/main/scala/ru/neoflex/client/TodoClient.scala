package ru.neoflex.client
import cats.effect.{ExitCode, IO, IOApp, Sync, Temporal}
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
import ru.neoflex.client.TodoClient.baseUrl

import scala.concurrent.duration.*
import scala.util.control.Breaks.*
import scala.io.StdIn.readLine
import Api.*
import com.comcast.ip4s.{Literals, SocketAddress}
import fs2.io.net.{ConnectException, Network, Socket}
import ru.neoflex.client.Cache.*
import ru.neoflex.client.Authorization
import fs2.{Stream, text}
import cats.effect.Temporal
import cats.effect.std.Console
import com.comcast.ip4s.Literals.host
import fs2.io.file.{Files, Path}
import cats.effect.Concurrent
import ru.neoflex.fs2.Fs2TransportFile



object TodoClient extends IOApp with Config :
  def run(args: List[String]): IO[ExitCode] =
    BlazeClientBuilder[IO].resource.use { client =>
      UI.start().unsafeRunSync()
      while (user == null) {
        val command = UI.authorization().unsafeRunSync()
        def sendUser(api: Uri, login: String, password: String) = {
          val user = User(login, password)
          (POST(user, registrationApi), user)
        }
        val post = command match {
          case Registration(login, password) =>
            sendUser(registrationApi, login, password)
          case Authorization(login, password) =>
            sendUser(authorizationApi, login, password)
          case _ => throw new Exception
        }

        if (client.successful(post._1).unsafeRunSync()) {
          user = post._2
        }
      }


      breakable {
        while (true) {
          val command = UI.selectOperation().unsafeRunSync()
          val request: IO[ExitCode] = command match {
            case SendNote(name, text, label) =>
              val userSession = user.getSession
              val item = TodoItemTmp(name, text, label, false, userSession)
              val postTodoItemAdd = POST(item, itemApiAdd)
              for
                status <- client.status(postTodoItemAdd)
                _ <- IO.println(s"Status: $status")
              yield
                ExitCode.Success

            case ShowNote() =>
              val postShowItem = GET(user, itemApiShow)
              for {
                list <- client.expect[List[TodoItem]](postShowItem)
                _ <- IO.delay({
                  Cache.notes = list
                })
                _ <- IO.println(UI.printTodoItem(list))
              } yield ExitCode.Success

            case EditNote(id, name, text, label, status) =>
              val updateNote = TodoItemTmp(name, text, label, status, user.getSession)

              val editNote = POST(updateNote, itemApiEdit(id))

              for
                status <- client.status(editNote)
                _ <- IO.println(s"Status: $status")
              yield
                ExitCode.Success

            case ShowNoteFilter(api) =>
              val postShowItem = GET(user, api)

              for {
                list <- client.expect[List[TodoItem]](postShowItem)
                _ <- IO.println(UI.printTodoItem(list))
              } yield ExitCode.Success

            case Delete(id) =>
              val postDeleteItem = POST(user, Api.itemApiDelete(id))
              for {
                status <- client.status(postDeleteItem)
                _ <- IO.println(s"Status: $status")
              } yield ExitCode.Success

            case UploadFile(openPort, pathToFile, nameFile) =>
              val postOpenPort = POST(Cache.user, openPort)
              val postCloseConnect = (portClose: String) => POST(Cache.user, Api.ftpApiClose(portClose))
              for
                port <- client.expect[String](postOpenPort)
                _ <- IO.println(port)
                _ <- Fs2TransportFile.sendFile[IO](port, pathToFile).compile.drain
                _ <- client.expect[String](postCloseConnect(port))
              yield ExitCode.Success

            case Exit() => break()

            case NotFoundCommand() =>
              for
                _ <- IO.println("Not found command")
              yield
                ExitCode.Success
          }

          request.unsafeRunSync()
        }
      }
      IO(ExitCode.Success)
    }
