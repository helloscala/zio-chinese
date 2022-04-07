package zionomicon.ch08

import zio._

object FiberFork2 extends ZIOAppDefault {
  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {
    val effect: ZIO[Clock, Nothing, Int] = for {
      _ <- ZIO
        .succeed(println("heartbeat"))
        .delay(1.second)
        .forever
//        .fork // 使用 fork，heartbeat 不会打印。需要使用 forkDaemon 来确保它可以在后台被执行，因为 fork 的 fiber 很快就会执行完成，以到来不及打印 heartbeat
        .forkDaemon // 若不使用 fork 或者 forkDaemon，那 "Doing some expensive work" 将不会被执行。
      _ <- ZIO.succeed(println("Doing some expensive work"))
    } yield 42

    val module = for {
      fiber <- effect.fork
      _ <- ZIO
        .succeed(println("Doing some other work"))
        .delay(5.seconds)
      result <- fiber.join
    } yield {
      println(s"result of module is $result")
      result
    }

    val program = for {
      fiber <- module.fork
      _ <- ZIO.succeed(println("Running another module entirely")).delay(10.seconds)
      _ <- fiber.join
    } yield ()

//    program.exitCode
    module.exitCode
  }
}
