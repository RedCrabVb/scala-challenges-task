import Dependency.version.scala3Version
import Dependency.{circe, doobie, fs2, http4s, scalaTest, scopt}
import sbt._

ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := scala3Version


lazy val root = Project(id = "ServerNotes", base = file("."))
  .disablePlugins(AssemblyPlugin)
  .settings(name := "ServerNotes")
  .aggregate(server, client)

lazy val serverMain = Some("ru.neoflex.server.NotesServer")

lazy val server = (project in file("./server"))
  .enablePlugins(AssemblyPlugin)
  .settings(
    mainClass in(Compile, run) := serverMain,
    mainClass in assembly := serverMain,
    assemblyJarName in assembly := "server.jar",
    libraryDependencies ++= http4s.http4s,
    libraryDependencies ++= circe.circe,
    libraryDependencies ++= fs2.fs2,
    libraryDependencies ++= doobie.doobie,
    libraryDependencies += scalaTest.scalaTest
  ).dependsOn(core)

lazy val clientMain = Some("ru.neoflex.client.NotesClient")

lazy val client = (project in file("./client"))
  .enablePlugins(AssemblyPlugin)
  .settings(
    mainClass in(Compile, run) := clientMain,
    mainClass in assembly := clientMain,
    assemblyJarName in assembly := "client.jar",
    libraryDependencies ++= http4s.http4s,
    libraryDependencies ++= circe.circe,
    libraryDependencies ++= fs2.fs2,
    libraryDependencies += scopt.scopt,
    libraryDependencies += scalaTest.scalaTest
  ).dependsOn(core)

lazy val core = (project in file("./core"))
  .settings(
      libraryDependencies ++= fs2.fs2,
  )

