package ru.neoflex.server

import doobie.*
import doobie.implicits.*
import cats.*
import cats.effect.*
import cats.implicits.*
import doobie.util.ExecutionContexts
import ru.neoflex.{Account, NotesTmp, Notes, Files}


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

