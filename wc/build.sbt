ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.1.0"

lazy val root = (project in file("."))
  .settings(
    libraryDependencies ++= Seq(
      Dependencies.fs2.fs2IO,
//      Dependencies.AkkaStream.akkaStream
      Dependencies.fs2.fs2Core
    ),
    name := "wc"
  )
