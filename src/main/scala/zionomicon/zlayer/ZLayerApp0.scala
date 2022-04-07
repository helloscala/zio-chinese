package zionomicon.zlayer

import zio._
import zio.console._
import zio.clock._
import java.io.IOException
import zio.duration.Duration._

object moduleA {
  type ModuleA = Has[ModuleA.Service]

  object ModuleA {
    trait Service {
      def letsGoA(v: Int): UIO[String]
    }

    val any: ZLayer[ModuleA, Nothing, ModuleA] =
      ZLayer.requires[ModuleA]

    val live: Layer[Nothing, Has[Service]] = ZLayer.succeed {
      new Service {
        def letsGoA(v: Int): UIO[String] = UIO(s"done: v = $v ")
      }
    }
  }

  def letsGoA(v: Int): URIO[ModuleA, String] =
    ZIO.accessM(_.get.letsGoA(v))
}

import moduleA._

object moduleB {
  type ModuleB = Has[ModuleB.Service]

  object ModuleB {
    trait Service {
      def letsGoB(v: Int): UIO[String]
    }

    val any: ZLayer[ModuleB, Nothing, ModuleB] =
      ZLayer.requires[ModuleB]

    val live: ZLayer[ModuleA, Nothing, ModuleB] = ZLayer.fromService { (moduleA: ModuleA.Service) =>
      new Service {
        def letsGoB(v: Int): UIO[String] =
          moduleA.letsGoA(v)
      }
    }
  }

  def letsGoB(v: Int): URIO[ModuleB, String] =
    ZIO.accessM(_.get.letsGoB(v))
}

object ZLayerApp0 extends zio.App {

  import moduleB._

  private val env = Console.live ++ Clock.live ++ (ModuleA.live >>> ModuleB.live)
  private val program: ZIO[Console with ModuleB with Clock, IOException, Unit] =
    for {
      _ <- putStrLn(s"Welcome to ZIO!")
      _ <- sleep(Finite(1000))
      r <- letsGoB(10)
      _ <- putStrLn(r)
    } yield ()

  def run(args: List[String]) =
    program.provideLayer(env).exitCode

}
