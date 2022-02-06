package ru.neoflex.server

import cats.Defer
import cats.effect.{Concurrent, ExitCode, Sync}
import io.circe.generic.auto.*
import io.circe.syntax.*
import cats.syntax.all.catsSyntaxApplicativeId

final case class TodoItem(id: Int,
                          name: String,
                          text: String,
                          label: String,
                          status: Boolean,
                          files: List[FileItem],
                          session: String
                         )

final case class TodoItemTmp(name: String, text: String, label: String, status: Boolean, session: String)

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

  def getAllItems[F[_]](user: User)(using Concurrent[F]): F[List[TodoItem]] = {
    items.filter(_.session == user.getSession).pure
  }

  def getItemsWithLabel[F[_]](user: User, filter: TodoItem => Boolean)(using Concurrent[F]): F[List[TodoItem]] = {
    items.filter(item => item.session == user.getSession && filter(item)).pure
  }

  def prependItems[F[_]: Concurrent](item: TodoItemTmp): F[TodoItem] = Concurrent[F].pure{
    checkSession(item.session)

    val newItem = TodoItem(items.size + 1, item.name, item.text, item.label, false, List(), item.session)
    items = newItem :: items
    newItem
  }

  def deleteNote[F[_]: Concurrent](user: User, id: Int): F[Unit] = Concurrent[F].pure{
    checkSession(user.getSession)

    items.find(_.id == id).getOrElse(throw new Exception("Not found"))

    items = items.filter(_.id != id)
  }

  def editItems[F[_]: Concurrent](itemTmp: TodoItemTmp, id: Int): F[TodoItem] = Concurrent[F].pure{
    checkSession(itemTmp.session)

    val itemInDB = items.filter(_.id == id).head
    val item = TodoItem(id, itemTmp.name, itemTmp.text, itemTmp.label, itemTmp.status, itemInDB.files, itemInDB.session)
    items = item :: items.filter(_.id != id)
    item
  }


  def sortItems[F[_]: Concurrent](f: TodoItem => String, session: String): F[List[TodoItem]] = Concurrent[F].pure{
    checkSession(session)

    items.filter(_.session == session).sortBy(f)
  }

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

  def checkSession(session: String): Unit = {
    users.find(_.getSession == session).getOrElse(throw new Exception("Not found user"))
  }