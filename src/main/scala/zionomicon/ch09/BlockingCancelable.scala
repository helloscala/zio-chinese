package zionomicon.ch09

import zio._

import java.util.concurrent.atomic.AtomicBoolean

object BlockingCancelable extends ZIOAppDefault {
  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {
    def effect(canceled: AtomicBoolean): URIO[Nothing, Any] => RIO[Any, (Unit, ZIO[Any, Nothing, Unit])] =
      ZIO.attemptBlockingCancelable(
        {
          var i = 0
          while (i < 100000 && !canceled.get()) {
            println(i)
            i += 1
          }
        },
        UIO.succeed(canceled.set(true))
      )

    val program = for {
      ref <- ZIO.succeed(new AtomicBoolean(false))
      fiber <- effect(ref).fork
      _ <- fiber.interrupt
    } yield ()
    program.exitCode
  }
}
