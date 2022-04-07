package zionomicon.ch02

import zio._

object CH02 extends ZIOAppDefault {
  def readFile(file: String): String = {
    val source = scala.io.Source.fromFile(file)
    try source.getLines.mkString("\n")
    finally source.close()
  }

  def readFileZio(file: String): Task[String] = ZIO.effect(readFile(file))

  def writeFile(file: String, text: String): Unit = {
    import java.io._
    val pw = new PrintWriter(new File(file))
    try pw.write(text)
    finally pw.close()
  }

  def writeFileZio(file: String, text: String): Task[Unit] = ZIO.effect(writeFile(file, text))

  def copyFileZio(srcFile: String, destFile: String): Task[Unit] = for {
    content <- readFileZio(srcFile)
    _ <- writeFileZio(destFile, content)
  } yield ()

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {
    val logic = for {
      content <- readFileZio(sys.props("user.home") + "/http_ca.crt")
      _ <- console.putStrLn(content)
    } yield ()
    logic.exitCode
  }
}
