package ru.neoflex.client

import cats.effect.IO
import ru.neoflex.Notes
import scopt.OptionParser
import ru.neoflex.client.{UploadFile, ShowNoteSort, ShowNoteFilter}

type NotesAndFile = List[(Notes, Option[ru.neoflex.Files])]

case class CommandFromConfig(
                          login: Option[String] = Some(null),
                          password: Option[String] = Some(null),
                          connectionStart: Command = Authorization(),
                          command: List[Command] = List(),
                          uiCommand: List[IO[Command]] = List()
                        )

object ConfigParser {
  def apply(): OptionParser[CommandFromConfig] = new OptionParser[CommandFromConfig]("client for REST") {
    head("ru/neoflex/client", "2.x")

    opt[String]('l', "login").text("login for service")
      .action((value, config) => config.copy(login = Option(value)))
    opt[String]('p', "password").text("password for service")
      .action((value, config) => config.copy(password = Option(value)))


    opt[String]("sort").text("sort notes by filed, example --sort text")
      .action((value, config) => {
        config.copy(command = ShowNoteSort(Api.noteApiSort(value)) :: config.command)
      })
    opt[Map[String, String]]("filter").text("show notes with filter, example: --filter status=true (!!!There must be one equal sign!!!)")
      .action((value, config) => config.copy(command = {
        val filed = value.keys.head
        ShowNoteFilter(Api.noteApiFilter(filed, value(filed))) :: config.command
      }))
    opt[Int]('c', "change").text("change notes, enter id")
      .action((value, config) => config.copy(command = EditNote(value) :: config.command))
    opt[Int]('d', "delete").text("delete notes, enter id")
      .action((value, config) => config.copy(command = Delete(value) :: config.command))
    opt[Unit]('s', "show").text("load and show notes")
      .action((value, config) => config.copy(command = ShowNote() :: config.command))
    opt[Seq[String]]('s', "uploadFile").text("enter path to file and name file on server, example: --uploadFile 33,local.txt,server.txt")
      .action((value, config) => {
        config.copy(command = UploadFile(Api.ftpApi(value.head.toInt, value(2)), value(1)) :: config.command)
      }
      )
    opt[Unit]("addNotes").text("add notes")
      .action((value, config) => config.copy(command = SendNote() :: config.command))
    opt[Unit]('a', "authorization").text("authorization attempt")
      .action((value, config) => config.copy(connectionStart = Authorization()))
    opt[Unit]('r', "registration").text("registration attempt")
      .action((value, config) => config.copy(connectionStart = Registration()))

    help("help").text(
      """
        |In the program you can:
        |* send notes
        |* attach to notes big files
        |* authorization and registration
        |""".stripMargin)

    override def showUsageOnError: Option[Boolean] = Some(true)
  }

}
