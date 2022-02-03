package neoflex.ru

import cats.effect.*
import fs2.Stream
import org.http4s.*
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.dsl.Http4sDsl

object Main extends IOApp {
  def run(args: List[String]): IO[ExitCode] =
    ExampleApp.serverStream[IO].compile.drain.as(ExitCode.Success)
}

object ExampleApp extends Config {
  def serverStream[F[_]: Async]: Stream[F, ExitCode] = {
    BlazeServerBuilder[F]
      .bindHttp(port, host)
      .withHttpApp(new ExampleRoutes[F].routes.orNotFound)
      .serve
  }
}

final case class ExampleRoutes[F[_]: Sync]() extends Http4sDsl[F] {
  val routes: HttpRoutes[F] =
    HttpRoutes.of[F] { case GET -> Root / "ping" =>
      Ok("ping")
    }
}