package ru.neoflex.server

import cats.effect.kernel.Deferred
import cats.effect.syntax.*
import cats.effect.{Concurrent, ExitCode, IO, IOApp}
import cats.implicits.*
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.implicits.*
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
import ru.neoflex.Config

object NotesServer extends IOApp with NotesRoutes with Config:
  import java.nio.file.Files
  import java.nio.file.Paths
  Files.createDirectories(Paths.get(userFolder))

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