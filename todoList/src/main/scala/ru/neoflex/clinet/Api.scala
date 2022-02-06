package ru.neoflex.clinet

import org.http4s.Uri
import ru.neoflex.clinet.TodoClient.baseUrl

object Api {
  private[this] val exception = new Exception("Not valid uri")
  val registrationApi: Uri = Uri.fromString(baseUrl + "/registration").getOrElse(throw exception)
  val authorizationApi: Uri = Uri.fromString(baseUrl + "/authorization").getOrElse(throw exception)
  val itemApiAdd: Uri = Uri.fromString(baseUrl + "/item").getOrElse(throw exception)
  val itemApiShow: Uri = Uri.fromString(baseUrl + "/itemShow").getOrElse(throw exception)

  def itemApiLabel(label: String): Uri = Uri.fromString(baseUrl + s"/item/$label").getOrElse(throw exception)

  def itemApiEdit(id: Int): Uri = Uri.fromString(baseUrl + "/item/edit/" + id).getOrElse(throw exception)

  def itemApiSort(id: Int): Uri = Uri.fromString(baseUrl + "/item/sort/" + id).getOrElse(throw exception)

  def itemApiFilter(id: Int): Uri = Uri.fromString(baseUrl + "/item/filter/" + id).getOrElse(throw exception)
}
