package ru.neoflex.client

import cats.effect.IO
import ru.neoflex.Notes
import scopt.OptionParser
import ru.neoflex.client.UploadFile

type NotesAndFile = List[(Notes, Option[ru.neoflex.Files])]


case class ConfigClient(login: String = "None",
                        password: String = "None",
                        showNotes: Boolean = false,
                        authorization: Boolean = false,
                        registration: Boolean = false,
                        addNotes: IO[Command] = IO(UnitCommand()),
                        changeNotes: (notes: NotesAndFile) => IO[Command] = (notes: NotesAndFile) => IO(UnitCommand()),
                        deleteNote: Int = -1,
                        showWithFilter: (String, String) = ("", ""),
                        sortByFiled: String = "",
                        uploadFile: (login: String) => Command = (login: String) => UnitCommand(),
                        loadFile: String = "None")

object ConfigParser {
  def apply() = new OptionParser[ConfigClient]("client for REST") {
    head("ru/neoflex/client", "1.x")

    opt[String]('l', "login").text("login for service")
      .action((value, config) => config.copy(value))
    opt[String]('p', "password").text("password for service")
      .action((value, config) => config.copy(password = value))
    opt[String]('f', "path").text("send the file to the server, additional data is required")
      .action((value, config) => config.copy(loadFile = value))
    opt[String]("sort").text("sort notes by filed, example --sort text")
      .action((value, config) => config.copy(sortByFiled = value))
    opt[String]("filter").text("show notes with filter, example: --filter status=true (!!!There must be one equal sign!!!)")
      .action((value, config) => config.copy(showWithFilter = {
        val str = value.split("=")
        (str(0), str(1))
      }))
    opt[Int]('c', "change").text("change notes, enter id")
      .action((value, config) => config.copy(changeNotes = (notes: NotesAndFile) => UI.editNote(value, notes)))
    opt[Int]('d', "delete").text("delete notes, enter id")
      .action((value, config) => config.copy(deleteNote = value))
    opt[Unit]('s', "show").text("load and show notes")
      .action((value, config) => config.copy(showNotes = true))
    opt[String]('s', "uploadFile").text("enter path to file and name file on server, example: --uploadFile 33|./local.txt|server.txt")
      .action((value, config) => {
        val pathAndNameAndId = value.split("\\|")
        val update: (String) => Command = (login: String) => UploadFile(
          Api.ftpApi(pathAndNameAndId(0).toInt, pathAndNameAndId(2), login),
          pathAndNameAndId(1),
        )
        config.copy(uploadFile = update)}
      )
    opt[Unit]("addNotes").text("add notes")
      .action((value, config) => config.copy(addNotes = UI.addNote()))
    opt[Unit]('a', "authorization").text("authorization attempt")
      .action((value, config) => config.copy(authorization = true))
    opt[Unit]('r', "registration").text("registration attempt")
      .action((value, config) => config.copy(registration = true))

    help("help").text(
      """
        |In the program you can:
        |* send notes
        |* attach to notes big files
        |* authorization and registration
        |""".stripMargin)
  }
}
