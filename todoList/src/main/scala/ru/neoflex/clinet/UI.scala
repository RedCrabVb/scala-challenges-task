package ru.neoflex.clinet

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
        |---2. Authorization---
        |---3. Registration---
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
      case "4" => RemoveFile()
      case "5" => UploadFile()
      case _ => ???
    }
  }
}
