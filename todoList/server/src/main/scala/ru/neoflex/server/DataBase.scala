package ru.neoflex.server

import doobie.*
import doobie.implicits.*
import cats.*
import cats.effect.*
import cats.implicits.*
import doobie.util.ExecutionContexts
import doobie.util.fragment.Fragment.const
import ru.neoflex.{Account, Files, Notes, NotesTmp}

object DataBase {
  def registration(login: String, password: String): doobie.Update0 = {
    sql"insert into account (login, password) values ($login, $password)".update
  }

  def authorization(login: String, password: String): doobie.ConnectionIO[Account] = {
    sql"select * from account where login = $login and password = $password".query[Account].unique
  }

  def deleteNote(id: Int, idUser: Int): doobie.Update0 = {
    sql"""delete from notes where id = $id and idUser = $idUser""".update
  }
  
  def addFile(idNote: Int, nameFile: String): doobie.Update0 = {
    sql"insert into files (idnotes, name) values ($idNote, $nameFile);".update
  }


  def addNote(idUser: Int, notesTmp: NotesTmp): doobie.Update0 = {
    sql"""insert into Notes (iduser, name, text, label, status)
         values ($idUser, ${notesTmp.name}, ${notesTmp.text}, ${notesTmp.label}, ${notesTmp.status})""".update
  }

  def sortByFiled(idUser: Int, filed: String): doobie.ConnectionIO[List[(Notes, Option[Files])]] = {
    sql"""select * from notes left join files on (notes.id = files.idnotes) where idUser = $idUser order by $filed"""
      .query[(Notes, Option[Files])].stream.compile.toList
  }

  def filterByValue(idUser: Int, filed: String, value: String): doobie.ConnectionIO[List[(Notes, Option[Files])]] = {
    val where = fr"""where idUser = $idUser and""" ++ const(s"$filed = '$value'")
    val select = sql"select * from notes left join files on (notes.id = files.idnotes)"
    val sql = select ++ where//possibly an sql-injection
    sql.query[(Notes, Option[Files])].stream.compile.toList
  }

  def editNote(id: Int, name: String, text: String, label: String, status: Boolean): doobie.Update0 = {
    sql"""update Notes set name = $name, text = $text, label = $label, status = $status where id = $id""".update
  }

  def getAllNotes(idUser: Int): doobie.ConnectionIO[List[(Notes, Option[Files])]] = {
    sql"select * from notes left join files on (notes.id = files.idnotes) where idUser =$idUser".query[(Notes, Option[Files])].stream.compile.toList
  }
}

