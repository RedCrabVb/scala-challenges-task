package ru.neoflex.server

import cats.Defer
import cats.effect.{Concurrent, Sync}
import io.circe.generic.auto._
import io.circe.syntax._
import cats.syntax.all.catsSyntaxApplicativeId

object Storage:
  private var items: List[TodoItem] = List[TodoItem]()

  def list[F[_]](using Sync[F]): F[List[TodoItem]] =
    items.pure

  def prepend[F[_]: Concurrent](item: TodoItemTmp): F[Unit] = Concurrent[F].pure{
    items = TodoItem(items.size + 1, item.text, item.label, List()) :: items
  }

  //todo: sort by id, status, countFile
  def sort[F[_]: Sync]: F[Unit] = {
    items = items.sortBy(_.text)
  }.pure