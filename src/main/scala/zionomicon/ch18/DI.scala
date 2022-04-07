package zionomicon.ch18

import zio._

object DI {
  def effect1: ZIO[Clock with Has[Console] with Has[Random], Nothing, Unit] = ???
}
