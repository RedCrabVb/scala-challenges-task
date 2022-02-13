package ru.neoflex.client

import cats.effect.{ExitCode, IO, IOApp, Sync, Temporal}
import cats.implicits._
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
import ru.neoflex.client.Authorization
import cats.effect.Temporal
import cats.effect.Concurrent
import ru.neoflex.fs2.Fs2TransportFile

object NotesClient extends IOApp with Config :
  def run(args: List[String]): IO[ExitCode] =
    BlazeClientBuilder[IO].resource.use { client =>

      ConfigParser().parse(args, ConfigCommand()) match {
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


          for {
            account <- client.successful(post._1).flatMap(if (_) { post._2} else {throw new Exception("Failed to connect")})
            notes <- client.expect[ru.neoflex.client.NotesAndFile](GET(account, noteApiLoad))
            _ <- config.command.map(_.createRequest(client, account)).sequence
          } yield ExitCode.Success
        case None =>
          ???
      }

    }
