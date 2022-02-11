package ru.neoflex.client

import org.http4s.Uri
import ru.neoflex.client.TodoClient.baseUrl

object Api {
  val uri: String => Uri = Uri.fromString(_).getOrElse(???)

  val registrationApi: Uri = uri(baseUrl + "/registration")
  val authorizationApi: Uri = uri(baseUrl + "/authorization")
  val noteApiAdd: Uri = uri(baseUrl + "/note/add")
  val noteApiLoad: Uri = uri(baseUrl + "/note/load")

  def notesApiDelete(id: Int): Uri = uri(baseUrl + "/note/delete/" + id)

  def noteApiEdit(id: Int): Uri = uri(baseUrl + "/note/edit/" + id)

  def noteApiSort(id: String): Uri = uri(baseUrl + "/note/sort/" + id)

  def noteApiFilter(filter: String, value: String): Uri = uri(baseUrl + s"/note/filter/$filter/$value")

  def ftpApi(id: String, nameFile: String, user: String): Uri = uri(baseUrl + s"/ftp/$user/$id/$nameFile")

  def ftpApiClose(port: String): Uri = uri(baseUrl + s"/ftp/$port")
}
