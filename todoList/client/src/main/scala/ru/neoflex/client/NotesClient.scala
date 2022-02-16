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

      ConfigParser().parse(args, CommandFromConfig()) match {
        case Some(commands) =>
          println(commands)

          for {
            account <- IO(Account(commands.login.get, commands.password.get))
            _ <- commands.connectionStart.createRequest(client, account)
            _ <- commands.command.map(_.createRequest(client, account)).reverse.sequence
          } yield ExitCode.Success
        case None => IO{ExitCode.Error}
      }

    }
