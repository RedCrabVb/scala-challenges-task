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
import doobie.*
import doobie.implicits.*
import cats.*
import cats.effect.*
import cats.implicits.*
import doobie.util.ExecutionContexts
import doobie.util.fragment.Fragment.const
import ru.neoflex.{Account, Files, Notes, NotesTmp}

object Storage extends Config:
  val xa = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver", // driver classname
    s"jdbc:postgresql:$dataBaseName", // connect URL (driver-specific)
    dataBaseUser,
    dataBasePassword
  )



  def addFile(idNote: Int, nameFile: String): IO[Int] = {
    sql"insert into files (idnotes, name) values ($idNote, $nameFile);".update.run.transact(xa)
  }

  def getAllNotes(account: Account): IO[List[(Notes, Option[Files])]] = {
    def getAllNotes(idUser: Int): doobie.ConnectionIO[List[(Notes, Option[Files])]] = {
      sql"select * from notes left join files on (notes.id = files.idnotes) where idUser =$idUser".query[(Notes, Option[Files])].stream.compile.toList
    }

    getAllNotes(account.id).transact(xa)
  }

  def prependNotes(account: Account, note: NotesTmp) = {
    sql"""insert into Notes (iduser, name, text, label, status)
         values (${account.id}, ${note.name}, ${note.text}, ${note.label}, ${note.status})""".update.run.transact(xa)
  }

  def deleteNotes(account: Account, id: Int): IO[Int] = {
    sql"""delete from notes where id = $id and idUser = ${account.id}""".update.run.transact(xa)
  }

  def editNotes(account: Account, noteTmp: NotesTmp, id: Int): IO[Int] = {
    def editNote(id: Int, name: String, text: String, label: String, status: Boolean):
    doobie.Update0 = {
      sql"""update Notes set name = $name, text = $text, label = $label, status = $status where id = $id""".update
    }

    editNote(id, noteTmp.name, noteTmp.text, noteTmp.label, noteTmp.status).run.transact(xa)
  }

  def sortNotes(field: String, account: Account): IO[List[(Notes, Option[Files])]] = {
    def sortByFiled(idUser: Int, filed: String):
    doobie.ConnectionIO[List[(Notes, Option[Files])]] = {
      sql"""select * from notes left join files on (notes.id = files.idnotes) where idUser = $idUser order by $filed"""
        .query[(Notes, Option[Files])].stream.compile.toList
    }

    sortByFiled(account.id, "notes." + field).transact(xa)
  }

  def getNotesWithFilter(account: Account, filed: String,  value: String): IO[List[(Notes, Option[Files])]] = {
    def filterByValue(idUser: Int, filed: String, value: String):
    doobie.ConnectionIO[List[(Notes, Option[Files])]] = {
      val where = fr"""where idUser = $idUser and""" ++ const(s"$filed = '$value'")
      val select = sql"select * from notes left join files on (notes.id = files.idnotes)"
      val sql = select ++ where//possibly an sql-injection
      sql.query[(Notes, Option[Files])].stream.compile.toList
    }

    filterByValue(account.id, "notes." + filed, value).transact(xa)
  }

  def registration(account: Account): IO[Unit] = {
    import java.nio.file.Files
    import java.nio.file.Paths
    for {
      _ <- sql"insert into account (login, password) values (${account.login}, ${account.password})".update.run.transact(xa)
      _ <- IO {
        Files.createDirectories(Paths.get(s"${userFolder}/${account.login}"))
      }
    } yield ()
  }

  def authentication(account: Account): IO[Account] = {
    sql"select * from account where login = ${account.login} and password = ${account.password}".query[Account].unique.transact(xa)
  }
