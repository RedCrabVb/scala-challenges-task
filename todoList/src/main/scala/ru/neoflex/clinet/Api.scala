package ru.neoflex.clinet

import org.http4s.Uri
import ru.neoflex.clinet.TodoClient.baseUrl

object Api {
  val registrationApi: Uri = Uri.fromString(baseUrl + "/registration").getOrElse(???)
  val authorizationApi: Uri = Uri.fromString(baseUrl + "/authorization").getOrElse(???)
  val itemApiAdd: Uri = Uri.fromString(baseUrl + "/item").getOrElse(???)
  val itemApiShow: Uri = Uri.fromString(baseUrl + "/itemShow").getOrElse(???)

  def itemApiDelete(id: Int): Uri = Uri.fromString(baseUrl + "/item/delete/" + id).getOrElse(???)

  def itemApiEdit(id: Int): Uri = Uri.fromString(baseUrl + "/item/edit/" + id).getOrElse(???)

  def itemApiSort(id: String): Uri = Uri.fromString(baseUrl + "/item/sort/" + id).getOrElse(???)

  def itemApiFilter(filter: String, value: String): Uri = Uri.fromString(baseUrl + s"/item/filter/$filter/$value").getOrElse(???)

  def ftpApi(id: String, nameFile: String, user: String): Uri = Uri.fromString(baseUrl + s"/ftp/$user/$id/$nameFile").getOrElse(???)

  def ftpApiClose(port: String): Uri = Uri.fromString(baseUrl + s"/ftp/$port").getOrElse(???)
}
