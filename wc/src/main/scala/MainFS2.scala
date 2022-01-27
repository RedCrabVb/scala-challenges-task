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

  val countLine: Stream[IO, Int] = lineSize.fold(0)((prev, next) => prev + 1)

  val maxLine: Stream[IO, Int] = lineSize
    .fold(0)((prev, next) => Math.max(next, prev))

  val charCount: Int => Stream[IO, Int] = (first: Int) => lineSize
    .fold(first)((prev, next) => prev + next)

  val result = countLine.onComplete(maxLine).onComplete(charCount(0)).compile.toList.unsafeRunSync()

  println("CountLine: " + (result(0).toInt - 1))
  println("MaxLine: " + result(1))
  println("CountChar: " + (result(2).toInt + result(0).toInt - 1))
}
