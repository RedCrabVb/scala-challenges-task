package ru.neoflex.clinet

import org.http4s.Uri
import ru.neoflex.server.{TodoItem, TodoItemTmp, User}


sealed class Command

final case class SendNote(name: String, text: String, label: String) extends Command

final case class ShowNote() extends Command

final case class ShowNoteFilter(api: Uri) extends Command

final case class EditNote(id: Int, name: String, text: String, label: String, status: Boolean) extends Command

final case class Delete(id: Int) extends Command

final case class RemoveFile() extends Command

final case class UploadFile(openPort: Uri, nameFile: String, pathFile: String) extends Command

final case class Authorization(login: String, password: String) extends Command

final case class Registration(login: String, password: String) extends Command

final case class Exit() extends Command

final case class NotFoundCommand() extends Command

object Cache {
  var user: User = _
  var notes: List[TodoItem] = _
}
