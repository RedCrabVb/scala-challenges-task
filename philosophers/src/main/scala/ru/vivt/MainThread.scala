package ru.vivt

case class Philosopher(name: String, forkLeft: Int, forkRight: Int)(implicit val forks: Array[Boolean]) {
  def eat(): Unit = {
    println("Start eating: " + name)
    Thread.sleep(100)
    println("End eating: " + name)
    println("Fork put: " + name)
    forkPut()
    println("To think: " + name)
  }

  def forkPut(): Unit = {
    forks(forkLeft) = true
    forks(forkRight) = true
  }
}


object MainThread extends App {
  implicit val forks: Array[Boolean] = Array.fill(5)(true)

  val philosopher = Array(Philosopher("Name 1", 0, 1),
    Philosopher("Name 2", 1, 2),
    Philosopher("Name 3", 2, 3),
    Philosopher("Name 4", 3, 4),
    Philosopher("Name 5", 4, 0)
  )

  while (true) {
    philosopher.foreach(p =>
      if (forks(p.forkRight) && forks(p.forkLeft)) {
        forks(p.forkLeft) = false
        forks(p.forkRight) = false
        new Thread(() => p.eat()).start()
      }
    )
  }

}
