import cats.effect.IO

import java.time.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import java.util.concurrent.Executors
import scala.io.{BufferedSource, Source}
import scala.sys.exit
import scala.util.{Failure, Success}

import cats.effect.IO

import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext
import cats.effect.unsafe.implicits.global

//https://raw.githubusercontent.com/benschw/shakespeare-txt/master/shakespeare-hamlet-25.txt
@main def main(wegPage: String): Unit = {
  val threadPull = Executors.newFixedThreadPool(2)
  val cpuPool: ExecutionContext = ExecutionContext.fromExecutor(threadPull)

  def countWorld(words: String) = IO {
    words.split(" ").length
  }

  def countChar(line: String) = IO {
    line.replace(" ", "").length
  }

  val result = (for {
    text <- IO { Source.fromURL(wegPage).getLines().mkString(" ") }
    words <- countWorld(text).evalOn(cpuPool)
    chars <- countChar(text).evalOn(cpuPool)
  } yield {
    chars.toFloat / words.toFloat
  }).unsafeRunSync()
  threadPull.shutdown()

  printf("%.1f\n", result)
}

