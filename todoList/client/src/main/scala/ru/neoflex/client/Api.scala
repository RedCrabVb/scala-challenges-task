package ru.neoflex.client

import org.http4s.Uri
import ru.neoflex.client.NotesClient.baseUrl

object Api {
  val uri: String => Uri = Uri.fromString(_).getOrElse(???)

  val registrationApi: Uri = uri(s"$baseUrl/registration")
  val authorizationApi: Uri = uri(s"$baseUrl/authorization")
  val noteApiAdd: Uri = uri(s"$baseUrl/note/add")
  val noteApiLoad: Uri = uri(s"$baseUrl/note/load")

  def notesApiDelete(id: Int): Uri = uri(s"$baseUrl/note/delete/$id")

  def noteApiEdit(id: Int): Uri = uri(s"$baseUrl/note/edit/" + id)

  def noteApiSort(id: String): Uri = uri(s"$baseUrl/note/sort/" + id)

  def noteApiFilter(filter: String, value: String): Uri = uri(s"$baseUrl/note/filter/$filter/$value")

  def ftpApi(id: Int, nameFile: String, user: String): Uri = uri(s"$baseUrl/ftp/$user/$id/$nameFile")

  def ftpApiClose(port: String): Uri = uri(s"$baseUrl/ftp/$port")
}
