import Dependency.version.scala3Version
import Dependency.{circe, fs2, http4s}

ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := scala3Version


lazy val root = (project in file("."))
  .settings(
    name := "todoList",
    libraryDependencies ++= http4s.http4s,
    libraryDependencies ++= circe.circe,
    libraryDependencies ++= fs2.fs2
  )
