package zionomicon.ch09

import zio._

object Interruption extends ZIOAppDefault {
  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {
    val program = for {
      fiber <- ZIO.succeed(println("Hello, World!")).fork
      _ <- fiber.interrupt
    } yield ()

//    val program = for {
//      ref <- Ref.make(false)
//      fiber <- ZIO.never.ensuring(ref.set(true)).fork
//      _ <- fiber.interrupt
//      value <- ref.get
//    } yield {
//      println(s"value is $value")
//      value
//    }

//    val program = for {
//      ref <- Ref.make(false)
//      promise <- Promise.make[Nothing, Unit]
//      fiber <- (promise.succeed(()) *> ZIO.never)
//        .ensuring(ref.set(true))
//        .fork
//      _ <- promise.await
//      _ <- fiber.interrupt
//      value <- ref.get
//    } yield {
//      println(s"value is $value")
//      value
//    }

    program.exitCode
  }
}
