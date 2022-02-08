package ru.neoflex.clinet

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
import ru.neoflex.clinet.TodoClient.baseUrl

import scala.concurrent.duration.*
import scala.util.control.Breaks.*
import scala.io.StdIn.readLine
import Api.*
import com.comcast.ip4s.{Literals, SocketAddress}
import fs2.io.net.{ConnectException, Network, Socket}
import ru.neoflex.clinet.Cache.*
import ru.neoflex.clinet.Authorization
import fs2.{Stream, text}
import cats.effect.Temporal
import cats.effect.std.Console
import com.comcast.ip4s.Literals.host
import fs2.io.file.{Files, Path}
import cats.effect.Concurrent

//todo:
// enter data for load on server,
// UI for select file load/download,
object TodoClient extends IOApp with Config :
  def connect[F[_]: Temporal: Network](address: com.comcast.ip4s.SocketAddress[com.comcast.ip4s.Host]): Stream[F, Socket[F]] =
    Stream.resource(Network[F].client(address))
      .handleErrorWith {
        case _: ConnectException =>
          connect(address).delayBy(5.seconds)
      }

  def sendFile[F[_]: Temporal: Network: Concurrent: Files](port: String, pathFile: String): Stream[F, Unit] =
    connect(SocketAddress(com.comcast.ip4s.Host.fromString(s"$host").get,
      com.comcast.ip4s.Port.fromString(port).get)).flatMap { socket =>
      Files[F].readAll(Path(pathFile))
        .through(socket.writes)
      }


  def run(args: List[String]): IO[ExitCode] =
//    for {
//      _ <- client[IO].compile.drain
//    } yield (ExitCode.Success)
    BlazeClientBuilder[IO].resource.use { client =>
      UI.start().unsafeRunSync()
      while (user == null) {
        val command = UI.authorization().unsafeRunSync()
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
          val command = UI.selectOperation().unsafeRunSync()
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
                _ <- IO.delay({
                  Cache.notes = list
                })
                _ <- IO.println(UI.printTodoItem(list))
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
            case ShowNoteFilter(api) =>
              val postTodoItems = GET(
                user,
                api
              )
              for {
                list <- client.expect[List[TodoItem]](postTodoItems)
                _ <- IO.println(UI.printTodoItem(list))
              } yield ExitCode.Success
            case Delete(id) =>
              val postTodoItems = POST(
                user,
                Api.itemApiDelete(id)
              )
              for {
                status <- client.status(postTodoItems)
                _ <- IO.println(s"Status: $status")
              } yield ExitCode.Success
            case UploadFile(openPort, nameFile) => {
              val postOpenPort = POST(
                Cache.user,
                openPort
              )
              val postCloseConnect = (portClose: String) => POST(
                Cache.user,
                Api.ftpApiClose(portClose)
              )
              for {
                port <- client.expect[String](postOpenPort)
                _ <- IO.println(port)
                _ <- sendFile[IO](port, nameFile).compile.drain
                _ <- client.expect[String](postCloseConnect(port))
              } yield ExitCode.Success
            }


            case Exit() => break()
            case _ =>
              for
                _ <- IO.println("Not found command")
              yield {
                ExitCode.Success
              }
          }

          request.unsafeRunSync()
        }
      }
      IO(ExitCode.Success)
    }
