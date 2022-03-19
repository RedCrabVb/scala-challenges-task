import doobie.*
import doobie.implicits.*
import cats.*
import cats.effect.*
import cats.implicits.*
import doobie.util.ExecutionContexts
import org.scalatest.funsuite.AnyFunSuite
import ru.neoflex.server.Storage
import ru.neoflex.Account
import cats.effect.unsafe.implicits.global
import org.scalatest.matchers.should
import org.scalatest.flatspec.AnyFlatSpec
import doobie.*
import doobie.implicits.*


class StorageTests extends AnyFlatSpec with should.Matchers:

  "storage" should "registration user" in {
    val account = Account("New_user", "for test")
    Storage.registration(account).unsafeRunSync()
    val result: Int = Storage.authentication(account).unsafeRunSync().id
    (sql"delete from account where id = $result".update).run.transact(Storage.xa).unsafeRunSync()
    (result > 0) should equal(true)
  }
