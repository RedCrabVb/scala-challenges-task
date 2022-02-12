import org.scalatest.funsuite.AnyFunSuite
import ru.neoflex.Account
import cats.effect.unsafe.implicits.global
import org.scalatest.matchers.should
import org.scalatest.flatspec.AnyFlatSpec
import ru.neoflex.client.Api.{noteApiLoad, uri}
import ru.neoflex.client.NotesClient.baseUrl


class ClientTest extends AnyFlatSpec with should.Matchers:

  "Api" should "create URI" in {
    noteApiLoad should be(uri(baseUrl + "/note/load"))
  }
