package zionomicon.zlayer

import zio._

object Example extends zio.App {

  def run(args: List[String]): URIO[ZEnv, ExitCode] =
    zio.provideLayer(nameLayer).as(ExitCode.success)

  private val zio = for {
    name <- ZIO.access[Has[String]](_.get)
    _ <- UIO(println(s"Hello, $name!"))
  } yield ()

  val nameLayer = ZLayer.succeed("羊八井")
}
