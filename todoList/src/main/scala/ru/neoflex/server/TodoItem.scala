package ru.neoflex.server

import cats.Applicative
import fs2.Chunk
import io.circe.{Decoder, Encoder, HCursor, Json}

//todo: status, id, data, files
final case class TodoItem(text: String)

final case class FileItem(path: String)

final case class LabelItem(list: List[TodoItem])

object TodoItem:

  given todoItemEncoder: Encoder[TodoItem] = (a: TodoItem) => Json.obj(
    ("text", Json.fromString(a.text)),
  )

  given todoItemDecoder: Decoder[TodoItem] = (c: HCursor) =>
    for
      text <- c.downField("text").as[String]
    yield
      TodoItem(text)