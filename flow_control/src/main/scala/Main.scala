import fs2.{INothing, Stream}
import cats.effect.{Deferred, IO}
import cats.effect.unsafe.implicits.global

import java.time.format.DateTimeFormatter
import scala.concurrent.duration.*

object Main extends App {

  val program = Stream.eval(Deferred[IO, Unit]).flatMap { _ =>
    var switchBool = true
    val switcher = Stream.
      repeatEval(
        for {
          ioInput <- IO.readLine
          result <- ioInput match {
            case "on" | "1" => IO { switchBool = true }
            case "off" | "0" => IO { switchBool = false }
            case _ => throw new UnsupportedOperationException
          }
        } yield result
      )

    val program =
      Stream.repeatEval(IO(
        if (switchBool) {
          val format = DateTimeFormatter.ofPattern("hh:mm:ss")
          println(java.time.LocalTime.now.format(format))
        }
      )).metered(2.second)

    program
      .concurrently(switcher)
  }

  program.compile.drain.unsafeRunSync()
}
