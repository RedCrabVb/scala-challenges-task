name := "philosophers"

version := "0.1"

scalaVersion := "3.1.0"

val AkkaVersion = "2.6.18"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion
)

mainClass in assembly := Some("ru.vivt.ProblemAboutPhilosophers")