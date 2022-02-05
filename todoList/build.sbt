val scala3Version = "3.1.0"

ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := scala3Version

val Http4sVersion = "1.0.0-M30" //"0.21.19"

val CirceVersion = "0.14.1"//"0.13.0"
val circe = Seq(
    "io.circe" %% "circe-core"    % CirceVersion,
    "io.circe" %% "circe-generic" % CirceVersion,
    "io.circe" %% "circe-parser"  % CirceVersion,
    //  "io.circe" %% "circe-literal" % CirceVersion,
)

lazy val root = (project in file("."))
  .settings(
    name := "todoList",
    libraryDependencies += "org.http4s" %% "http4s-core" % Http4sVersion,
    libraryDependencies += "org.http4s" %% "http4s-dsl" % Http4sVersion,
    libraryDependencies += "org.http4s" %% "http4s-circe" % Http4sVersion,
    libraryDependencies += "org.http4s" %% "http4s-blaze-server" % Http4sVersion,
    libraryDependencies += "org.http4s" %% "http4s-blaze-client" % Http4sVersion,
    libraryDependencies ++= circe
  )