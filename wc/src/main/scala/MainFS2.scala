import cats.effect.{IO, IOApp}
import fs2.{Pure, Stream, text}
import fs2.io.file.{Files, Path}
import cats.effect.unsafe.implicits.global

import java.io.FileInputStream

@main
def Main(name: String) = {
  //Pure, emit - Чистый, излучающий
  //through, Pipe - через, трубка

  val file: Stream[IO, Byte] =
    fs2.io.readInputStream(IO {
      new FileInputStream(name)
    }, chunkSize = 4096, closeAfterUse = true)

  val maxLine = file
    .through(text.utf8Decode)
    .through(text.lines)
    .map(_.length)

  val result = {
    maxLine.compile.toList.map(_.max)
  }
  println(result.unsafeRunSync())
}
