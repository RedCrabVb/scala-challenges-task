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

  val lineSize: Stream[IO, Int] = file
    .through(text.utf8Decode)
    .through(text.lines)
    .map(_.length)

  case class State(countLine: Int, maxLine: Int, countChar: Int) {
    def combine(addState: State): State = {
      State(countLine + addState.countLine, Math.max(maxLine, addState.maxLine), countChar + addState.countChar)
    }
  }


  val resultStateIO: IO[State] = lineSize
    .fold(State(-1, 0, -1))((state, l) => state.combine(State(1, l, l + 1)))
    .compile.toList.map(_.head)

  val resultState = resultStateIO.unsafeRunSync()

  println("CountLine: " + resultState.countLine)
  println("MaxLine: " + resultState.maxLine)
  println("CountChar: " + resultState.countChar)
}
