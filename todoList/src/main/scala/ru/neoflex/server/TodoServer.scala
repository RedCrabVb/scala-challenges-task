package ru.neoflex.server

import cats.effect.syntax.*
import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits.*
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.implicits.*
import ru.neoflex.Config

object TodoServer extends IOApp with TodoListRoutes[IO] with Config:

  val app = (
    itemsRoutes <+>
      authorizationRoutes
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