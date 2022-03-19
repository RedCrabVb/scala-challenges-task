package ru.vivt

object Data {
  private[this] var typeScenario: Int = 0
  def typeScenarioGet(): Int = typeScenario
  val countPhilosopher = 5

  def apply(typeScenario: Int): Unit = {
    this.typeScenario = typeScenario
  }

}






