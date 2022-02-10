package ru.neoflex.server

import cats.Defer
import cats.effect.{Concurrent, ExitCode, IO, Sync}
import io.circe.generic.auto.*
import io.circe.syntax.*
import cats.syntax.all.catsSyntaxApplicativeId
import fs2.concurrent.SignallingRef
import fs2.io.file.Path
import ru.neoflex.server.TodoServer.{portFtp}
import cats.effect.unsafe.implicits.global
import ru.neoflex.fs2.Fs2TransportFile
import doobie._
import doobie.implicits._
import cats._
import cats.effect._
import cats.implicits._
import doobie.util.ExecutionContexts


import scala.collection.mutable
import scala.collection.mutable.ListBuffer


object Storage:
  val xa = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver",     // driver classname
    "jdbc:postgresql:crudnote",     // connect URL (driver-specific)
    "postgres",                  // Account
    ""                           // password
  )


//  def addFile(id: Int, nameFile: String, account: Account): Unit = {
//    ???
//  }

//  def getItemByIdAndSession(id: Int, session: String): Notes = {
//    ???
//  }


//  def getAllItems[F[_]](account: Account)(using Concurrent[F]): F[List[(Notes, Option[Files])]] = {
//    DataBase.getAllNotes(account.id).transact(xa).unsafeRunSync()
//  }.pure
//
//  def getNotesWithLabel[F[_]](Account: Account, filter: Notes => Boolean)(using Concurrent[F]): F[List[Notes]] = {
//    ???
//  }
//
  def prependNotes(account: Account, notes: NotesTmp) = {
    DataBase.addNote(getIdAccount(account.login, account.password), notes).run.transact(xa)
  }
//
//  def deleteNotes[F[_] : Concurrent](Account: Account, id: Int): F[Unit] = Concurrent[F].pure {
//    ???
//  }
//
//
//  def editItems[F[_] : Concurrent](itemTmp: NotesTmp, id: Int): F[Notes] = Concurrent[F].pure {
//    ???
//  }
//
//  def sortItems[F[_] : Concurrent](f: Notes => String, session: String): F[List[Notes]] = Concurrent[F].pure {
//    ???
//  }


  def registration(account: Account): IO[Unit] = {
    import java.nio.file.Files
    import java.nio.file.Paths
    for {
      _ <- DataBase.registration(account.login, account.password).run.transact(xa)
      _ <-  IO{Files.createDirectories(Paths.get(s"${TodoServer.userFolder}/${account.login}"))}
    } yield ()
  }

  def authorization(account: Account): IO[Account] = {
    DataBase.authorization(account.login, account.password).transact(xa)
  }

  private def getIdAccount(login: String, password: String): Int = {
    DataBase.authorization(login, password).transact(xa).unsafeRunSync().id
  }