package ru.neoflex.server

import cats.effect.kernel.Deferred
import cats.effect.syntax.*
import cats.effect.{Concurrent, ExitCode, IO, IOApp}
import cats.implicits.*
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.implicits.*
import ru.neoflex.Config
import fs2.{INothing, Stream, text}
import fs2.io.file.{Files, Path}
import fs2.io.net.{Network, Socket}
import cats.effect.std.Console

import scala.concurrent.duration.*
import cats.effect.unsafe.implicits.global
import com.comcast.ip4s.Port
import fs2.concurrent.SignallingRef
import fs2.io.file.Files
import ru.neoflex.server.Storage

import java.time.format.DateTimeFormatter
import scala.collection.mutable
//todo: remove file, download file
object TodoServer extends IOApp with TodoListRoutes[IO] with Config:

  def socketRead(port: String, out: Path, interrupter: fs2.concurrent.SignallingRef[IO, Boolean]) =
    Network[IO].server(port = Port.fromString(port)).map { client =>
      client.reads
        .through(Files[IO].writeAll(out))
        .handleErrorWith(_ => Stream.empty)
      }.parJoin(10).interruptWhen(interrupter)


  val app = (
    itemsRoutes <+>
      authorizationRoutes <+>
      ftpRoutes
    ).orNotFound

  val server = BlazeServerBuilder[IO]
    .bindHttp(port)
    .withHttpApp(app)

  val serverResource = server.resource

  def run(args: List[String]): IO[ExitCode] =
    server
      .serve
      .compile.drain
      .as(ExitCode.Success)