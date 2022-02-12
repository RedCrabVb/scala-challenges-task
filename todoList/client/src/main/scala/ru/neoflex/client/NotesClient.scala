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
import ru.neoflex.client.NotesClient.baseUrl

import scala.concurrent.duration.*
import scala.util.control.Breaks.*
import scala.io.StdIn.readLine
import Api.*
import ru.neoflex.Files
import com.comcast.ip4s.{Literals, SocketAddress}
import fs2.io.net.{ConnectException, Network, Socket}
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

object NotesClient extends IOApp with Config :
  def run(args: List[String]): IO[ExitCode] =
    BlazeClientBuilder[IO].resource.use { client =>

      ConfigParser().parse(args, ConfigClient()) match {
        case Some(config) =>
          println(config)

          def sendUser(api: Uri, login: String, password: String) = {
            val account = Account(login, password)
            (POST(account, api), IO {
              account
            })
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
                val postNotesAdd = POST((account, item), noteApiAdd)
                for
                  status <- client.status(postNotesAdd)
                  _ <- IO.println(s"Status: $status")
                yield
                  ExitCode.Success

              case EditNote(id, name, text, label, status) =>
                val updateNote = (account, NotesTmp(name, text, label, status))
                val editNote = POST(updateNote, noteApiEdit(id))
                for
                  status <- client.status(editNote)
                  _ <- IO.println(s"Status: $status")
                yield
                  ExitCode.Success

              case UploadFile(api, path) =>
                for
                  port <- client.expect[String](POST(account, api))
                  _ <- IO.println(port)
                  _ <- Fs2TransportFile.sendFile[IO](port.toInt, path).compile.drain
                  _ <- IO.sleep(10.seconds)
                  _ <- client.expect[String](POST(account, Api.ftpApiClose(port)))
                yield ExitCode.Success

              case UnitCommand() => IO {
                ExitCode.Success
              }
          }

          for {
            account <- client.successful(post._1).flatMap(if (_) { post._2} else {throw new Exception("Failed to connect")})
            notes <- client.expect[NotesAndFile](GET(account, noteApiLoad))
            _ <- IO(config.showNotes).flatMap(if (_) IO.println(UI.printNotes(notes)) else IO.unit)
            statusDelete <- IO(config.deleteNote != -1).flatMap {
              if (_) {
                client.status(POST(account, Api.notesApiDelete(config.deleteNote)))
              } else IO.unit
            }
            _ <- IO(config.sortByFiled.nonEmpty).flatMap(if (_) {
              for
                sortNote <- client.expect[NotesAndFile](GET(account, Api.noteApiSort(config.sortByFiled)))
                _ <- IO.println(UI.printNotes(sortNote))
              yield ()
            } else IO.unit)
            _ <- IO(config.showWithFilter._1.nonEmpty).flatMap(if (_) {
              for
                sortNote <- client.expect[NotesAndFile](GET(account, Api.noteApiFilter(config.showWithFilter._1, config.showWithFilter._2)))
                _ <- IO.println(UI.printNotes(sortNote))
              yield ()
            } else IO.unit)
            addNotes <- config.addNotes
            changeNotes <- config.changeNotes(notes)
            uploadFile <- IO(config.uploadFile(account.login))
            _ <- createRequest(addNotes, account)
            _ <- createRequest(changeNotes, account)
            _ <- createRequest(uploadFile, account)
          } yield ExitCode.Success
        case None =>
          ???
      }

    }
