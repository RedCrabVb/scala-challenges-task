package ru.neoflex.server

import cats.Defer
import cats.effect.{Concurrent, ExitCode, IO, Sync}
import io.circe.generic.auto.*
import io.circe.syntax.*
import cats.syntax.all.catsSyntaxApplicativeId
import fs2.concurrent.SignallingRef
import fs2.io.file.Path
import ru.neoflex.server.TodoServer.{portFtp, userFolder}
import cats.effect.unsafe.implicits.global

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

final case class TodoItem(id: Int,
                          session: String,
                          name: String = "Not found",
                          text: String = "Not found",
                          label: String = "Not found",
                          status: Boolean = false,
                          files: List[String] = List()
                         ) {
  def toTodoItemTmp(): TodoItemTmp = {
    TodoItemTmp(name, text, label, status, session)
  }

  def update(name: String = "Not found",
             text: String = "Not found",
             label: String = "Not found",
             status: Boolean = false,
             files: List[String] = List()): TodoItem = {
    TodoItem(id, session, name, text, label, status, files)
  }
}

final case class TodoItemTmp(name: String, text: String, label: String, status: Boolean, session: String) {
  def toTodoItem(id: Int): TodoItem = {
    TodoItem(id, session, name, text, label, status)
  }
}

final case class FileItem(path: String)

final case class LabelItem(name: String, list: List[TodoItem])

final case class User(login: String, password: String) {
  def getSession: String = {
    (login.hashCode + password.hashCode).toString
  }
}


object Storage:
  private var id = 0

  private def getId: Int = {
    id += 1
    id
  }

  private var items: List[TodoItem] = List[TodoItem]()
  private var users: List[User] = List[User]()


  //-------- fixme: to IO
  private val ftpPortAndPath: mutable.Map[String, Path] = {
    val map = scala.collection.mutable.Map("5555" -> Path("userFolder/error"))
    portFtp.foreach(p => map(p.toString) = Path(s"$userFolder/none"))
    map
  }
  private val ftpPortBlock: Map[String, SignallingRef[IO, Boolean]] = portFtp.map(x => x.toString ->
    SignallingRef[IO, Boolean](true).unsafeRunSync()
  ).toMap

  def addFile(id: Int, nameFile: String, user: User): Unit = {
    val item = getItemByIdAndSession(id, user.getSession)
    val newItem = item.update(files = List(nameFile) ++ item.files)
    items = newItem :: items.filter(_.id != id)
  }

  def blockPort(fileName: String, user: User): String = {
    checkSession(user.getSession)

    val port = ftpPortBlock.find(_._2.get.unsafeRunSync() == true)
      .getOrElse(throw new Exception("Not found free port"))._1
    ftpPortAndPath(port) = Path(s"$userFolder/${user.login}/$fileName")
    ftpPortBlock(port).getAndSet(false).unsafeRunSync()
    TodoServer.socketRead(port, ftpPortAndPath(port), ftpPortBlock(port)).compile.drain.unsafeRunAsync(_ => ())
    port
  }

  def unblockPort(port: String, user: User): Unit = {
    checkSession(user.getSession)
    ftpPortBlock(port).getAndSet(true).unsafeRunSync()
  }

  //--------


  def checkSession(session: String): Unit = {
    users.find(_.getSession == session).getOrElse(throw new Exception("Not found user"))
  }

  def getItemByIdAndSession(id: Int, session: String): TodoItem = {
    items.find(item => item.id == id && item.session == session).getOrElse(throw new Exception("Not found"))
  }


  def getAllItems[F[_]](user: User)(using Concurrent[F]): F[List[TodoItem]] = {
    items.filter(_.session == user.getSession).pure
  }

  def getItemsWithLabel[F[_]](user: User, filter: TodoItem => Boolean)(using Concurrent[F]): F[List[TodoItem]] = {
    items.filter(item => item.session == user.getSession && filter(item)).pure
  }

  def prependItems[F[_] : Concurrent](item: TodoItemTmp): F[TodoItem] = Concurrent[F].pure {
    checkSession(item.session)

    val newItem = TodoItem(getId, item.session, item.name, item.text, item.label)
    items = newItem :: items
    newItem
  }

  def deleteTodoItem[F[_] : Concurrent](user: User, id: Int): F[Unit] = Concurrent[F].pure {
    checkSession(user.getSession)

    getItemByIdAndSession(id, user.getSession)

    items = items.filter(_.id != id)
  }

  def editItems[F[_] : Concurrent](itemTmp: TodoItemTmp, id: Int): F[TodoItem] = Concurrent[F].pure {
    checkSession(itemTmp.session)

    val itemInDB = getItemByIdAndSession(id, itemTmp.session)
    val item = itemTmp.toTodoItem(id)
    items = item :: items.filter(_.id != id)
    item
  }

  def sortItems[F[_] : Concurrent](f: TodoItem => String, session: String): F[List[TodoItem]] = Concurrent[F].pure {
    checkSession(session)

    items.filter(_.session == session).sortBy(f)
  }


  def registration[F[_] : Concurrent](user: User): F[Unit] = Concurrent[F].pure {
    if (users.map(_.login).contains(user.login)) {
      throw new Exception("Use with this login already exist")
    }
    users = user :: users
    import java.nio.file.Files
    import java.nio.file.Paths
    Files.createDirectories(Paths.get(s"$userFolder/${user.login}"))
  }

  def authorization[F[_] : Concurrent](userForCheck: User): F[Unit] = Concurrent[F].pure {
    val realAccount = users.map(_.getSession).contains(userForCheck.getSession)
    if (!realAccount) {
      throw new NoSuchElementException()
    }
  }