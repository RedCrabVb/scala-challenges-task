package ru.neoflex.server

import doobie._
import doobie.implicits._
import cats._
import cats.effect._
import cats.implicits._
import doobie.util.ExecutionContexts


final case class Account(login: String, password: String, id: Int = -1)
final case class Notes(idUser: Int = -1, name: String = "Not found",
                       text: String = "Not found", label: String = "Not found",
                       status: Boolean = false, id: Int = -1)
final case class Files(idNote: Int, name: String)

final case class NotesTmp(name: String, text: String, label: String, status: Boolean)

object DataBase {
  def registration(login: String, password: String): doobie.Update0 = {
    sql"insert into account (login, password) values ($login, $password)".update
  }

  def authorization(login: String, password: String): doobie.ConnectionIO[Account] = {
    sql"select * from account where login = $login and password = $password".query[Account].unique
  }

  def addNote(idUser: Int, notesTmp: NotesTmp): doobie.Update0 = {
    sql"""insert into Notes (iduser, name, text, label, status)
         values ($idUser, ${notesTmp.name}, ${notesTmp.text}, ${notesTmp.label}, ${notesTmp.status})""".update
  }

  def editNote(id: Int, name: String, text: String, label: String, status: Boolean): doobie.Update0 = {
    sql"""update Notes set name = $name, text = $text, label = $label, status = $status where id = $id""".update
  }

  def getAllNotes(idUser: Int): doobie.ConnectionIO[List[(Notes, Option[Files])]] = {
    sql"select * from notes left join files on (notes.id = files.idnotes) where idUser =$idUser".query[(Notes, Option[Files])].stream.compile.toList
  }
}

