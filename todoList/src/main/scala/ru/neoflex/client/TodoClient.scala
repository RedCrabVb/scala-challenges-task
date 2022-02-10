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
import ru.neoflex.server.{Account, Notes, NotesTmp}
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
import scopt.OParser
import scopt.OptionParser


case class ConfigClient(login: String = "None",
                  password: String = "None",
                  showNotes: Boolean = false,
                  changeNotes: Int = -1,
                  loadFile: String = "None")


object TodoClient extends IOApp with Config :
  def run(args: List[String]): IO[ExitCode] =
    BlazeClientBuilder[IO].resource.use { client =>
      val parser = new OptionParser[ConfigClient]("client for REST") {
        head("client", "1.x")

        opt[String]('l', "login").text("login for service")
          .action((value, config) => config.copy(value))
        opt[String]('p', "password").text("password for service")
          .action((value, config) => config.copy(password = value))
        opt[String]('f', "path").text("send the file to the server, additional data is required")
          .action((value, config) => config.copy(loadFile = value))
        opt[Int]('c', "change").text("change notes, enter id")
          .action((value, config) => config.copy(changeNotes = value))
        opt[Unit]('s', "show").text("load and show notes")
          .action((value, config) => config.copy(showNotes = true))

        help("help").text("prints this usage text")
      }

      parser.parse(args, ConfigClient()) match {
        case Some(config) =>
          //      Picker.choose(config.objects, config.count).foreach(println)
          println(config)


        case None =>
        // arguments are bad, error message will have been displayed
      }


      UI.start().unsafeRunSync()
      while (user == null) {
        val command = UI.authorization().unsafeRunSync()
        def sendUser(api: Uri, login: String, password: String) = {
          val account = Account(login, password)
          (POST(account, api), account)
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
              val item = NotesTmp(name, text, label, false)
              val postNotesAdd = POST((user, item), itemApiAdd)
              for
                status <- client.status(postNotesAdd)
                _ <- IO.println(s"Status: $status")
              yield
                ExitCode.Success

            case ShowNote() =>
              val postShowItem = GET(user, itemApiShow)
              for {
                list <- client.expect[List[(Notes, Option[ru.neoflex.server.Files])]](postShowItem)
                _ <- IO.delay({
//                  Cache.notes = list
                })
                _ <- IO.println(UI.printNotes(list))
              } yield ExitCode.Success

            case EditNote(id, name, text, label, status) =>
              val updateNote = NotesTmp(name, text, label, status)

              val editNote = POST(updateNote, itemApiEdit(id))

              for
                status <- client.status(editNote)
                _ <- IO.println(s"Status: $status")
              yield
                ExitCode.Success

            case ShowNoteFilter(api) =>
              val postShowItem = GET(user, api)

              for {
                list <- client.expect[List[(Notes, Option[ru.neoflex.server.Files])]](postShowItem)
                _ <- IO.println(UI.printNotes(list))
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
