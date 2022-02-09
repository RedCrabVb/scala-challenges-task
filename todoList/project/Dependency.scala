import Dependency.version.{circeVersion, doobieVersion, fs2CoreVersion, http4sVersion}
import sbt._

object Dependency {
  object version {
    val scala3Version = "3.0.0"
    val http4sVersion = "1.0.0-M30"
    val fs2CoreVersion = "3.2.4"
    val circeVersion = "0.14.1"
    val doobieVersion = "1.0.0-RC2"
  }

  object http4s {
    val http4s = Seq("org.http4s" %% "http4s-core" % http4sVersion,
      "org.http4s" %% "http4s-dsl" % http4sVersion,
      "org.http4s" %% "http4s-circe" % http4sVersion,
      "org.http4s" %% "http4s-blaze-server" % http4sVersion,
      "org.http4s" %% "http4s-blaze-client" % http4sVersion)
  }

  object circe {
    val circe = Seq(
      "io.circe" %% "circe-core" % circeVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-parser" % circeVersion,
    )
  }

  object fs2 {
    val fs2 = Seq("co.fs2" %% "fs2-core" % fs2CoreVersion,
      "co.fs2" %% "fs2-io" % fs2CoreVersion,
      "co.fs2" %% "fs2-reactive-streams" % fs2CoreVersion,
      "co.fs2" %% "fs2-scodec" % fs2CoreVersion)
  }

  object doobie {
    val doobie = Seq(
      "org.tpolecat" %% "doobie-core"     % doobieVersion,
      "org.tpolecat" %% "doobie-postgres" % doobieVersion
    )
  }
}
