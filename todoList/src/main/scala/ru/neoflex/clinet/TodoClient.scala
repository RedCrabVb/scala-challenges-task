package ru.neoflex.clinet

import cats.effect.{ExitCode, IO, IOApp}
import org.http4s.{MediaType, Request, client, *}
import org.http4s.client.*
import org.http4s.blaze.client.BlazeClientBuilder

import scala.concurrent.ExecutionContext.global
import org.http4s.client.dsl.io.*
import org.http4s.headers.*
import org.http4s.Method.*
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.circe.CirceEntityCodec.circeEntityDecoder
import org.http4s.circe.*
import ru.neoflex.Config
import ru.neoflex.server.{TodoItem, TodoItemTmp}
import io.circe.generic.auto.*
import io.circe.syntax.*

import scala.io.StdIn.readLine

sealed class Command
final case class SendNote(name: String, label: String) extends Command
final case class RemoveFile() extends Command
final case class UploadFile() extends Command
final case class Authorization() extends Command
final case class Registration() extends Command

object UI {
  def start() = {
    println(
      """
        |In the program you can:
        |* send notes
        |* attach to notes big files
        |* authorization and registration
        |""".stripMargin)
  }

  def selectOperation(): Command = {
    println("Select command: ")
    println(
      """
        |1. Send note
        |2. Authorization
        |3. Registration
        |4. Remove file
        |5. Upload file
        |""".stripMargin)
    val commandStr = readLine()
    commandStr match {
      case "1" => {
        println("Enter name note")
        val name = readLine()
        println("Enter label note")
        val label = readLine()
        SendNote(name, label)
      }
      case "2" => Authorization()
      case "3" => Registration()
      case "4" => RemoveFile()
      case "5" => UploadFile()
      case _ => ???
    }
  }
}


//todo: registration in app,
// enter data for load on server,
// UI for select file load/download,
// UI for show user todoItem
object TodoClient extends IOApp with Config:

  def run(args: List[String]): IO[ExitCode] =
    BlazeClientBuilder[IO].resource.use { client =>
      UI.start()
      val command = UI.selectOperation()
      val post = command match {
        case SendNote(name, label) => {
          val item = TodoItemTmp(name, label)
          val itemApi = Uri.fromString(baseUrl + "/item").getOrElse(???)
          val postTodoItem = POST(
            item,
            itemApi
          )
          (postTodoItem, baseUrl+"/items")
        }
        case _ => ???
      }

      for
        status <- client.status(post._1)
        _ <- IO{ println(s"Post status: $status") }
        items <- client.expect[List[TodoItemTmp]](post._2)
        _ <- IO{ println("Answer: " + items.mkString(" ")) }
      yield
        ExitCode.Success
    }

