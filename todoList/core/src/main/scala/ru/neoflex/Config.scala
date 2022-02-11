package ru.neoflex

trait Config {
  val port = 8080
  val host = "localhost"
  val baseUrl = s"http://$host:$port"
  
  val portsFtp = List(9080, 9081, 9082, 9083)
  val userFolder = "./userFolder"

  val dataBaseName = "crudnote"
  val dataBaseUser = "postgres"
  val dataBasePassword = ""
}
