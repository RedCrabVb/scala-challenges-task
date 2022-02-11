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
import ru.neoflex.{Account, Notes, NotesTmp}
import io.circe.generic.auto.*
import io.circe.syntax.*
import cats.effect.unsafe.implicits.global
import ru.neoflex.client.TodoClient.baseUrl

import scala.concurrent.duration.*
import scala.util.control.Breaks.*
import scala.io.StdIn.readLine
import Api.*
import ru.neoflex.Files
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
                        authorization: Boolean = false,
                        registration: Boolean = false,
                        addNotes: IO[Command] = IO(UnitCommand()),
                        changeNotes: IO[Command] = IO(UnitCommand()),
                        loadFile: String = "None")


object TodoClient extends IOApp with Config :
  def run(args: List[String]): IO[ExitCode] =
    BlazeClientBuilder[IO].resource.use { client =>
      val parser = new OptionParser[ConfigClient]("client for REST") {
        head("ru/neoflex/client", "1.x")

        opt[String]('l', "login").text("login for service")
          .action((value, config) => config.copy(value))
        opt[String]('p', "password").text("password for service")
          .action((value, config) => config.copy(password = value))
        opt[String]('f', "path").text("send the file to the server, additional data is required")
          .action((value, config) => config.copy(loadFile = value))
        opt[Int]('c', "change").text("change notes, enter id")
          .action((value, config) => config.copy(changeNotes = UI.editNote(value)))
        opt[Unit]('s', "show").text("load and show notes")
          .action((value, config) => config.copy(showNotes = true))
        opt[Unit]("addNotes").text("add notes")
          .action((value, config) => config.copy(addNotes = UI.addNote()))
        opt[Unit]('a', "authorization").text("authorization attempt")
          .action((value, config) => config.copy(authorization = true))
        opt[Unit]('r', "registration").text("registration attempt")
          .action((value, config) => config.copy(registration = true))

        help("help").text(
          """
            |In the program you can:
            |* send notes
            |* attach to notes big files
            |* authorization and registration
            |""".stripMargin)
      }

      parser.parse(args, ConfigClient()) match {
        case Some(config) =>
          println(config)

          def sendUser(api: Uri, login: String, password: String) = {
            val account = Account(login, password)
            (POST(account, api), IO{account})
          }

          val post = if (config.registration) {
            sendUser(registrationApi, config.login, config.password)
          } else if (config.authorization) {
            sendUser(authorizationApi, config.login, config.password)
          } else {
            throw new Exception("Select the connection option (authorization or registration)")
          }

          def createRequest(command: Command, account: Account): IO[ExitCode] = {
            command match
              case SendNote(name, text, label) =>
                val item = NotesTmp(name, text, label, false)
                val postNotesAdd = POST((account, item), itemApiAdd)
                for
                  status <- client.status(postNotesAdd)
                  _ <- IO.println(s"Status: $status")
                yield
                  ExitCode.Success

              case EditNote(id, name, text, label, status) =>
                val updateNote = NotesTmp(name, text, label, status)
                val editNote = POST(updateNote, itemApiEdit(id))
                for
                  status <- client.status(editNote)
                  _ <- IO.println(s"Status: $status")
                yield
                  ExitCode.Success

              case UnitCommand() => IO{ExitCode.Success}
          }

          val request: IO[ExitCode] = for {
            account <- client.successful(post._1).flatMap(result =>
              if (result) {
                post._2
              } else {
                ???
              }
            )
            notes <- client.expect[List[(Notes, Option[ru.neoflex.Files])]](GET(account, itemApiShow))
            _ <- IO{config.showNotes}.flatMap(result =>
              if(result) IO.println(UI.printNotes(notes)) else IO.unit
            )
            addNotes <- config.addNotes
            changeNotes <- config.changeNotes
            _ <- createRequest(addNotes, account)
            _ <- createRequest(changeNotes, account)
          } yield ExitCode.Success

          request
        case None =>
          ???
      }

//      breakable {
//        while (false) {
//          val command = UI.selectOperation().unsafeRunSync()
//          val request: IO[ExitCode] = command match {
//            case SendNote(name, text, label) =>
//              val item = NotesTmp(name, text, label, false)
//              val postNotesAdd = POST((user, item), itemApiAdd)
//              for
//                status <- client.status(postNotesAdd)
//                _ <- IO.println(s"Status: $status")
//              yield
//                ExitCode.Success
//
//
//            case EditNote(id, name, text, label, status) =>
//              val updateNote = NotesTmp(name, text, label, status)
//
//              val editNote = POST(updateNote, itemApiEdit(id))
//
//              for
//                status <- client.status(editNote)
//                _ <- IO.println(s"Status: $status")
//              yield
//                ExitCode.Success
//
//            case ShowNoteFilter(api) =>
//              val postShowItem = GET(user, api)
//
//              for {
//                list <- client.expect[List[(Notes, Option[ru.neoflex.Files])]](postShowItem)
//                _ <- IO.println(UI.printNotes(list))
//              } yield ExitCode.Success

//            case Delete(id) =>
//              val postDeleteItem = POST(user, Api.itemApiDelete(id))
//              for {
//                status <- client.status(postDeleteItem)
//                _ <- IO.println(s"Status: $status")
//              } yield ExitCode.Success
//
//            case UploadFile(openPort, pathToFile, nameFile) =>
//              val postOpenPort = POST(Cache.user, openPort)
//              val postCloseConnect = (portClose: String) => POST(Cache.user, Api.ftpApiClose(portClose))
//              for
//                port <- client.expect[String](postOpenPort)
//                _ <- IO.println(port)
//                _ <- Fs2TransportFile.sendFile[IO](port, pathToFile).compile.drain
//                _ <- client.expect[String](postCloseConnect(port))
//              yield ExitCode.Success
//          }

//          request.unsafeRunSync()
//        }
//      }
    }
