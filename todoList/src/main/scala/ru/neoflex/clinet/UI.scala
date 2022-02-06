package ru.neoflex.clinet

import ru.neoflex.server.TodoItem

import scala.io.StdIn.readLine

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

  def authorization(): Command = {
    println("Authorization on registration in account")
    println("Select command: ")
    println(
      """
        |1. Authorization
        |2. Registration""".stripMargin)
    val commandStr = readLine()
    commandStr match {
      case "1" =>
        println("Enter your login")
        val login = readLine()
        println("Enter your password")
        val password = readLine()
        Authorization(login, password)
      case "2" =>
        println("Enter your login")
        val login = readLine()
        println("Enter your password")
        val password = readLine()
        Registration(login, password)
    }
  }

  def selectOperation(): Command = {
    println("Select command: ")
    println(
      """
        |1. Send note
        |2. Show notes
        |3. Show notes with for filter
        |4. Edit note
        |-5. Remove file
        |-6. Upload file
        |7. Exit
        |""".stripMargin)
    val commandStr = readLine()
    commandStr match {
      case "1" =>
        println("Enter name note")
        val name = readLine()
        println("Enter text")
        val text = readLine()
        println("Enter label note")
        val label = readLine()

        SendNote(name, text, label)
      case "2" => ShowNote()
      case "3" =>
        println("Filter and sort select: ")
        println(
          """
            |1. Filter label
            |2. Sort size text
            |3. Filter status
            |4. Sort on text""".stripMargin)
        val select = readLine()
        val uri = select match {
          case "1" =>
            println("Enter label: ")
            val value = readLine()
            Api.itemApiFilter("label", value)
          case "2" =>
            Api.itemApiSort("text")
          case "3" =>
            println("Enter status for filter (true/false)")
            val statusForFilter = readLine().toLowerCase
            Api.itemApiFilter("status", statusForFilter)
          case "4" =>
            Api.itemApiSort("text")
          case _ => ???
        }
        ShowNoteFilter(uri)
      case "4" =>
        println("Enter id note")
        val id: Int = readLine().toInt
        println("Enter name note")
        val name = readLine()
        println("Enter text")
        val text = readLine()
        println("Enter label note")
        val label = readLine()
        println("Task completed? true/false")
        val status = readLine().toLowerCase().toBoolean
        EditNote(id, name, text, label, status)
      case "5" => RemoveFile()
      case "6" => UploadFile()
      case "7" => Exit()
      case _ => ???
    }
  }
}
