package zionomicon.ch09

import zio._

object SideEffect extends ZIOAppDefault {
  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {
    val effect: UIO[Unit] = UIO.succeed {
      var i = 0
      while (i < 100000) {
        Thread.sleep(50)
        println(i)
        i += 1
      }
    }

    val program = for {
      fiber <- effect.fork
      _ <- fiber.interrupt
    } yield ()

    program.exitCode
  }
}
