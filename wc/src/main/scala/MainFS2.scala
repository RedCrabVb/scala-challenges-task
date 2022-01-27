import cats.effect.{IO, IOApp}
import fs2.{Pure, Stream, text}
import fs2.io.file.{Files, Path}
import cats.effect.unsafe.implicits.global

import java.io.FileInputStream

@main
def Main(name: String) = {

  val file: Stream[IO, Byte] =
    fs2.io.readInputStream(IO {
      new FileInputStream(name)
    }, chunkSize = 4096, closeAfterUse = true)

  val lineSize = file
    .through(text.utf8Decode)
    .through(text.lines)
    .map(_.length)

  val countLine = lineSize.fold(1)((prev, next) => prev + 1)

  val maxLine = lineSize
    .fold(0)((prev, next) => Math.max(next, prev))

  val charCount = (first: Int) => lineSize
    .fold(first)((prev, next) => prev + next)

  val resultCountLine = countLine.compile.toList.map(_.head).unsafeRunSync().toInt
  val resultCountByte = charCount(resultCountLine).compile.toList.map(_.head).unsafeRunSync().toInt
  val resultMaxLine = maxLine.compile.toList.map(_.head).unsafeRunSync().toInt

  println("CountLine: " + resultCountLine)
  println("MaxLine: " + resultMaxLine)
  println("CountByte: " + resultCountByte)
}
