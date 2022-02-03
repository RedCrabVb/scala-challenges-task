package ru.neoflex

trait Config {
  val port = 8080
  val host = "localhost"
  val baseUrl = s"http://$host:$port"
}
