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

  val lineSize = file
    .through(text.utf8Decode)
    .through(text.lines)
    .map(_.length)
    .compile.toList

  val countLine =
    lineSize
      .map(_.size)

  val maxLine = lineSize
    .map(_.max)

  val charCount = lineSize
    .map(_.sum)

  val resultCountLine: Int = countLine.unsafeRunSync().toInt - 1
  val resultCountChar: Int = charCount.unsafeRunSync() + resultCountLine

  println("CountLine: " + resultCountLine)
  println("MaxLine: " + maxLine.unsafeRunSync())
  println("CountChar: " + resultCountChar)
}
