import Dependencies.fs2

ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.1.0"

lazy val root = (project in file("."))
  .settings(
    libraryDependencies ++= Seq(
      fs2.fs2Core,
      fs2.fs2IO
    ),
    name := "flow_control"
  )
