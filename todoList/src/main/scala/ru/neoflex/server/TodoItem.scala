package ru.neoflex.server

import cats.Applicative
import fs2.Chunk
import io.circe.generic.auto._

import java.util.Date

//todo: status, id, files
final case class TodoItem(id: Int, text: String, label: String, files: List[FileItem])

final case class TodoItemTmp(text: String, label: String)

final case class FileItem(path: String)

final case class LabelItem(name: String, list: List[TodoItem])

