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
import cats.effect.unsafe.implicits.global

import scala.io.StdIn.readLine

import ru.neoflex.server.User

sealed class Command
final case class SendNote(name: String, label: String) extends Command
final case class RemoveFile() extends Command
final case class UploadFile() extends Command
final case class Authorization(login: String, password: String) extends Command
final case class Registration(login: String, password: String) extends Command

object UI {
  def start(): Unit = {
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
      case "1" =>
        println("Enter name note")
        val name = readLine()
        println("Enter label note")
        val label = readLine()
        SendNote(name, label)
      case "2" =>
        println("Enter your login")
        val login = readLine()
        println("Enter your password")
        val password = readLine()
        Authorization(login, password)
      case "3" =>
        println("Enter your login")
        val login = readLine()
        println("Enter your password")
        val password = readLine()
        Registration(login, password)
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
  var user: User = null

  def run(args: List[String]): IO[ExitCode] =
    BlazeClientBuilder[IO].resource.use { client =>
      UI.start()
      while (true) {
        val command = UI.selectOperation()
        val post = command match {
          case SendNote(name, label) =>
            val userSession = Option(user).getOrElse(User("_", "_")).getSession
            val item = TodoItemTmp(name, label, userSession)
            val itemApi = Uri.fromString(baseUrl + "/item").getOrElse(???)
            val postTodoItem = POST(
              item,
              itemApi
            )
            (postTodoItem, baseUrl + "/items")
          case Registration(login, password) =>
            user = User(login, password)
            val itemApi = Uri.fromString(baseUrl + "/registration").getOrElse(???)
            val postTodoItem = POST(
              user,
              itemApi
            )
            (postTodoItem, baseUrl + "/items")
          case Authorization(login, password) =>
            val user = User(login, password)
            val itemApi = Uri.fromString(baseUrl + "/authorization").getOrElse(???)
            val postTodoItem = POST(
              user,
              itemApi
            )
            (postTodoItem, baseUrl + "/items")
          case _ => ???
        }

        val request: IO[ExitCode] = for
          status <- client.status(post._1)
          _ <- IO {
            println(s"Post status: $status")
          }
          items <- client.expect[List[TodoItem]](post._2)
          _ <- IO {
            println("Answer: " + items.mkString(" "))
          }
        yield
          ExitCode.Success

        request.unsafeRunSync()
      }
      ???
    }

