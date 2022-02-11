package ru.neoflex.server

import cats.Defer
import cats.effect.{Concurrent, ExitCode, IO, Sync}
import io.circe.generic.auto.*
import io.circe.syntax.*
import cats.syntax.all.catsSyntaxApplicativeId
import fs2.concurrent.SignallingRef
import fs2.io.file.Path
import ru.neoflex.server.NotesServer.portsFtp
import cats.effect.unsafe.implicits.global
import doobie.*
import doobie.implicits.*
import cats.*
import cats.effect.*
import cats.implicits.*
import doobie.util.ExecutionContexts
import ru.neoflex.{Account, Config, Files, Notes, NotesTmp}
import ru.neoflex.fs2.Fs2TransportFile

import scala.collection.mutable
import scala.collection.mutable.ListBuffer


object Storage extends Config:
  val xa = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver", // driver classname
    s"jdbc:postgresql:$dataBaseName", // connect URL (driver-specific)
    dataBaseUser,
    dataBasePassword
  )


  def addFile(id: Int, nameFile: String, account: Account): IO[Unit] = {
    IO{???}
  }

  //  def getItemByIdAndSession(id: Int, session: String): Notes = {
  //    ???
  //  }


  def getAllItems(account: Account): IO[List[(Notes, Option[Files])]] = {
    DataBase.getAllNotes(account.id).transact(xa)
  }

  def getNotesWithLabel(account: Account, filter: Notes => Boolean): IO[List[(Notes, Option[Files])]] = {
//    DataBase.getAllNotes(account.id).transact(xa)
    IO{???}
  }

  def prependNotes(account: Account, notes: NotesTmp) = {
    DataBase.addNote(getIdAccount(account.login, account.password), notes).run.transact(xa)
  }

  def deleteNotes(Account: Account, id: Int): IO[Unit] = {
    IO{???}
  }


  def editItems(itemTmp: NotesTmp, id: Int): IO[Int] = {
    DataBase.editNote(id, itemTmp.name, itemTmp.text, itemTmp.label, itemTmp.status).run.transact(xa)
  }

  def sortItems(f: Notes => String, session: String): IO[List[Notes]] = {
    IO{???}
  }


  def registration(account: Account): IO[Unit] = {
    import java.nio.file.Files
    import java.nio.file.Paths
    for {
      _ <- DataBase.registration(account.login, account.password).run.transact(xa)
      _ <- IO {
        Files.createDirectories(Paths.get(s"${userFolder}/${account.login}"))
      }
    } yield ()
  }

  def authorization(account: Account): IO[Account] = {
    DataBase.authorization(account.login, account.password).transact(xa)
  }

  private def getIdAccount(login: String, password: String): Int = {
    DataBase.authorization(login, password).transact(xa).unsafeRunSync().id
  }