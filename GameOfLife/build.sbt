ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.8"

scalaVersion := "3.1.0"

val AkkaVersion = "2.6.18"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion
)

lazy val root = (project in file("."))
  .settings(
name := "GameOfLife"
)

mainClass in assembly := Some("ru.vivt.Main")