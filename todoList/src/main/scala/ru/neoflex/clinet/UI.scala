package ru.neoflex.clinet

import ru.neoflex.server.TodoItem

import scala.io.StdIn.readLine
import scala.util.Try
import scala.util.hashing.Hashing.Default

object UI {
  private[this] def sendNote(): Command = {
    println("Enter name note")
    val name = readLine()
    println("Enter text")
    val text = readLine()
    println("Enter label note")
    val label = readLine()

    SendNote(name, text, label)
  }

  private[this] def filterAndSort(): Command = {
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
  }

  private[this] def editNote(): Command = {
    def changeThisField(field: String, value: => String): Boolean = {
      println("Current data: " + value)
      println(s"Change $field? true/false (default false)")
      Try {
        readLine().toLowerCase.toBoolean
      } getOrElse {
        false
      }
    }

    //fixme: code duplication
    def getValue(filed: String, value: => String): String = {
      if (changeThisField(filed, value)) {
        println(s"Enter $filed")
        readLine()
      } else {
        value
      }
    }

    def getValueBoolean(filed: String, value: => Boolean): Boolean = {
      if (changeThisField(filed, value.toString)) {
        println(s"Enter $filed note (true/false)")
        readLine().toLowerCase.toBoolean
      } else {
        value
      }
    }

    println("Enter id note")
    val id: Int = readLine().toInt
    val note = Cache.notes.find(_.id == id).head

    val name = getValue("name", note.name)
    val text = getValue("text", note.text)
    val label = getValue("label", note.label)
    val status = getValueBoolean("completed", note.status)

    EditNote(id, name, text, label, status)
  }

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
    println("\n\nSelect command: ")
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
        sendNote()
      case "2" => ShowNote()
      case "3" =>
        filterAndSort()
      case "4" =>
        editNote()
      case "5" => RemoveFile()
      case "6" => UploadFile()
      case "7" => Exit()
      case _ => ???
    }
  }

  def printTodoItem(list: List[TodoItem]): String = {
    "\n\n\n\n\n\n\n-----------------\n" + (for (item <- list) yield {
      s"""id: ${item.id}
         |name: ${item.name}
         |text: ${item.text}
         |label: ${item.label}
         |status: ${item.status}
         |files: ${item.files.mkString(", ")}
         |-----------------""".stripMargin
    }).mkString("\n")
  }
}
