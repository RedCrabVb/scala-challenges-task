package ru.neoflex.fs2

import cats.effect.kernel.Async
import cats.effect.{Concurrent, IO, Temporal}
import com.comcast.ip4s.{Port, SocketAddress}
import fs2.Stream
import fs2.concurrent.SignallingRef
import fs2.io.file.{Files, Path}
import fs2.io.net.{ConnectException, Network, Socket}
//import ru.neoflex.server.Storage.{getItemByIdAndSession}
import ru.neoflex.server.TodoServer.{portFtp, userFolder}
import ru.neoflex.server.{Account, Notes}
import cats.effect.unsafe.implicits.global
import ru.neoflex.client.TodoClient.host

import scala.concurrent.duration.*
import scala.collection.mutable

//todo: remove file, download file
object Fs2TransportFile {
  private val ftpPortAndPath: mutable.Map[String, Path] = {
    val map = scala.collection.mutable.Map("5555" -> Path("userFolder/error"))
    portFtp.foreach(p => map(p.toString) = Path(s"$userFolder/none"))
    map
  }
  private val ftpPortBlock: Map[String, SignallingRef[IO, Boolean]] = portFtp.map(x => x.toString ->
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
