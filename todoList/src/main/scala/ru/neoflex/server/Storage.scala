package ru.neoflex.server

import cats.Defer
import cats.effect.{Concurrent, Sync}
import io.circe.generic.auto._
import io.circe.syntax._
import cats.syntax.all.catsSyntaxApplicativeId

final case class TodoItem(id: Int,
                          text: String,
                          label: String,
                          files: List[FileItem],
                          session: String
                         )

final case class TodoItemTmp(text: String, label: String, session: String)

final case class FileItem(path: String)

final case class LabelItem(name: String, list: List[TodoItem])

final case class User(login: String, password: String) {
  def getSession: String = {
    (login.hashCode + password.hashCode).toString
  }
}


object Storage:
  private var items: List[TodoItem] = List[TodoItem]()
  private var users: List[User] = List[User]()

  def getAllItems[F[_]](using Sync[F]): F[List[TodoItem]] =
    items.pure

  def prependItems[F[_]: Concurrent](item: TodoItemTmp): F[TodoItem] = Concurrent[F].pure{
    users.find(_.getSession == item.session).getOrElse(throw new Exception("Not found user"))
    val newItem = TodoItem(items.size + 1, item.text, item.label, List(), item.session)
    items = newItem :: items
    newItem
  }

  //todo: sort by id, status, countFile
  def sortItems[F[_]: Sync]: F[Unit] = {
    items = items.sortBy(_.text)
  }.pure



  def registration[F[_]: Concurrent](user: User): F[Unit] = Concurrent[F].pure{
    if (users.map(_.login).contains(user.login)) {
      throw new Exception("Use with this login already exist")
    }
    users = user :: users
  }

  def authorization[F[_]: Concurrent](userForCheck: User): F[Unit] = Concurrent[F].pure{
    val realAccount = users.map(_.getSession).contains(userForCheck.getSession)
    if (!realAccount) {
      throw new NoSuchElementException()
    }
  }