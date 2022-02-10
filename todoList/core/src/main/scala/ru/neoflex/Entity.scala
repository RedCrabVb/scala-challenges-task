package ru.neoflex

final case class Account(login: String, password: String, id: Int = -1)
final case class Notes(idUser: Int = -1, name: String = "Not found",
                       text: String = "Not found", label: String = "Not found",
                       status: Boolean = false, id: Int = -1)
final case class Files(idNote: Int, name: String)

final case class NotesTmp(name: String, text: String, label: String, status: Boolean)