import Dependencies.version.{AkkaVersion, fs2CoreVersion, fs2IOVersion}
import sbt._

object Dependencies {
  object version {
    val fs2CoreVersion = "3.2.0"
    val fs2IOVersion = "3.2.0"
    val AkkaVersion = "2.6.18"
  }

  object fs2 {
    val fs2Core = "co.fs2" %% "fs2-core" % fs2CoreVersion
    val fs2IO = "co.fs2" %% "fs2-io" % fs2IOVersion
  }

  object AkkaStream {
    val akkaStream = "com.typesafe.akka" %% "akka-stream" % AkkaVersion
  }
}
