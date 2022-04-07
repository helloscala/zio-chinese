package zionomicon.ch03

import zio.test._
import zio.random._
import zio.test.Assertion._

object ExampleSpec extends DefaultRunnableSpec {
  val intGen = Gen.anyInt
  override def spec = suite("ExampleSpec")(
    testM("integer addition is associative") {
      check(intGen, intGen, intGen) { (x, y, z) =>
        val left = (x + y) + z
        val right = x + (y + z)
        assert(left)(equalTo(right))
      }
    }
  )
}
