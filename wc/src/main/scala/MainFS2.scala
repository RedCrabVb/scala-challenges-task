import cats.effect.{IO, IOApp}
import fs2.{Pure, Stream, text}
import fs2.io.file.{Files, Path}

import java.io.{FileInputStream, InputStream}

@main
def Main(file: String) = {
  //Pure, emit - Чистый, излучающий
  //through, Pipe - через, трубка

//  def fib(prev: BigInt, b: BigInt): Stream[Pure, BigInt] =
//    Stream.emit(b) ++ fib(b, prev + b)
//
//  val fib01 = fib(0, 1)
//  println(fib01.take(5).toList)


//    val sourceFile = scala.io.Source.fromFile(file).getLines().mkString("\n")
//
//    println("bytes: " + sourceFile.size)
//    println("char: " + sourceFile.length)
//    println("max line size: " + sourceFile.split("\n").map(_.length).max)
//    println("line: " + sourceFile.split("\n").length)
//    println("word: " + sourceFile.split("\n").flatMap(_.split("\\W+")).length)
//
//
//

}
