package ru.neoflex.fs2

import cats.effect.kernel.Async
import cats.effect.{Concurrent, IO, Temporal}
import com.comcast.ip4s.{Port, SocketAddress}
import fs2.Stream
import fs2.concurrent.SignallingRef
import fs2.io.file.{Files, Path}
import fs2.io.net.{ConnectException, Network, Socket}
import ru.neoflex.{Account, Config}
import cats.effect.unsafe.implicits.global

import scala.collection.mutable
import scala.concurrent.duration.*

//todo: remove file, download file
object Fs2TransportFile extends Config {
  private val ftpPortAndPath: mutable.Map[String, Path] = {
    val map = scala.collection.mutable.Map("5555" -> Path("userFolder/error"))
    portsFtp.foreach(p => map(p.toString) = Path(s"$userFolder/none"))
    map
  }
  private val ftpPortBlock: Map[String, SignallingRef[IO, Boolean]] = portsFtp.map(x => x.toString ->
    SignallingRef[IO, Boolean](true).unsafeRunSync()
  ).toMap

  def blockPort(fileName: String, user: Account): String = {
//    checkSession(user.getSession)

    val port = ftpPortBlock.find(_._2.get.unsafeRunSync() == true)
      .getOrElse(throw new Exception("Not found free port"))._1
    ftpPortAndPath(port) = Path(s"$userFolder/${user.login}/$fileName")
    ftpPortBlock(port).getAndSet(false).unsafeRunSync()
    Fs2TransportFile.socketRead[IO](port, ftpPortAndPath(port), ftpPortBlock(port)).compile.drain.unsafeRunAsync(_ => ())
    port
  }

  def unblockPort(port: String, user: Account): IO[Boolean] = {
//    checkSession(user.getSession)
    ftpPortBlock(port).getAndSet(true)
  }


  def socketRead[F[_]](port: String, out: Path, interrupter: fs2.concurrent.SignallingRef[F, Boolean])(using Network[F], Async[F]) =
    Network[F].server(port = Port.fromString(port)).map { client =>
      client.reads
        .through(Files[F].writeAll(out))
        .handleErrorWith(_ => Stream.empty)
    }.parJoin(10).interruptWhen(interrupter)

  def connect[F[_]: Temporal: Network](address: com.comcast.ip4s.SocketAddress[com.comcast.ip4s.Host]): Stream[F, Socket[F]] =
    Stream.resource(Network[F].client(address))
      .handleErrorWith {
        case _: ConnectException =>
          connect(address).delayBy(5.seconds)
      }

  def sendFile[F[_]: Temporal: Network: Concurrent: Files](port: String, pathFile: String): Stream[F, Unit] =
    connect(SocketAddress(com.comcast.ip4s.Host.fromString(s"$host").get,
      com.comcast.ip4s.Port.fromString(port).get)).flatMap { socket =>
      Files[F].readAll(Path(pathFile))
        .through(socket.writes)
    }
}
