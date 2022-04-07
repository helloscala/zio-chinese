package zionomicon.ch08

import zio._

object FiberFork extends ZIOAppDefault {
  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {
    val grandChild: UIO[Unit] = ZIO.succeed(println("Hello, World!"))

    val child: UIO[Unit] = grandChild.fork.flatMap(c => c.join)

    val process = child.fork // *> ZIO.unit
    process.exitCode
  }
}
