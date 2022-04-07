# 创建 Effect 

*原文：[https://zio.dev/next/overview/overview_creating_effects](https://zio.dev/next/overview/overview_creating_effects)*

本章探索从普通值、常见 Scala 类型，以及同步和异常副作用创建 ZIO Effect（效果）的一些常用方法。

## 从成功值

使用 `ZIO.succeed` 方法，你可以创建具有指定值的一个成功的 Effect ：
```
val s1 = ZIO.Succeed(42)
```

你也可以使用 `ZIO` 类型别名的伴身对象里的方法：
```
val s2: Task[Int] = Task.succeed(42)
```

`succeed` 方法采用传名参数，以确保构造值的任何副作用都可以被 ZIO 运行时正确管理。

## 从故障值

使用 `ZIO.fail` 方法，你可以创建一个建模为失败的 Effect ：
```
val f1 = ZIO.fail("Uh oh!")
```

对于 `ZIO` 数据类型，错误类型不存在限制。你可以在你的应用程序中使用合适使用字符串、异常或者自定义类型。

许多应用程序将扩展了 `Throwable` 或 `Exception` 的类对故障进行建模：
```
val f2 = Task.fail(new Exception("Uh oh!"))
```

注意，因为 `UIO` 值不能失败，`UIO` 不像其它 Effect 的伴身对象那样有 `UIO.fail` 方法。

## 从 Scala 值

Scala 的标准库包含大量数据类型，它们能够被转换为 ZIO Effect 。

### Option

`Option` 可以使用 `ZIO.fromOption` 转换为 ZIO Effect ：
```
val zoption: IO[Option[Nothing], Int] = ZIO.fromOption(Some(2))
```

所产的 Effect 的错误类型是 `Option[Nothing]]`，它没有提供为什么该值不存在的信息。你可以使用 `ZIO#mapError` 将 `Option[Nothing]` 改为更具体的错误类型。
```
val zoption2: IO[String, Int] = zoption.mapError(_ => "It wasn't there!")
```

你也可以很容易地将它与其它操作符组合，同时保留结果的可选性质（类似于 `OptionT`）
```
val maybeId: IO[Option[Nothing], String] = ZIO.fromOption(Some("abc123"))
def getUser(userId: String): IO[Throwable, Option[User]] = ???
def getTeam(teamId: String): IO[Throwable, Team] = ???

val result: IO[Throwable, Option[(User, Team)]] = (for {
  id   <- maybeId
  user <- getUser(id).some
  team <- getTeam(user.teamId).asSomeError 
} yield (user, team)).unsome 
```

### Either

`Either` 可以使用 `ZIO.fromEither` 转换为 ZIO Effect ：
```
val zeither = ZIO.fromEither(Right("Success!"))
```

所产生 Effect 的错误类型将是 `Left` 的任何类型，而成功类型将是 `Right` 的任何类型。

### Try

`Try` 可以使用 `ZIO.fromTry` 转换为 ZIO Effect ：
```
import scala.util.Try

val ztry = ZIO.fromTry(Try(42 / 0))
```

所产生 Effect 的错误类型总是 `Throwable`，因为 `Try` 只能以 `Throwable` 类型失败。

### Future

`Future` 可以使用 `ZIO.fromFuture` 转换为 ZIO Effect ：
```
import scala.concurrent.Future

lazy val future = Future.successful("Hello!")

val zfuture: Task[String] =
  ZIO.fromFuture { implicit ec =>
    future.map(_ => "Goodbye!")
  }
```

传给 `fromFuture` 的函数被传递了一个 `ExecutionContext`，它允许 ZIO 管理 `Future` 的运行位置（当然，你可以忽略这个 `ExecutionContext`）。

所生成 Effect 的错误类型总是 `Throwable`，因为 `Future` 只能以 `Throwable` 类型失败。

## 从副作用

ZIO 可以转换同步及异步副作用为 ZIO Effect （纯值）。

这些函数可用于包装程序性代码，允许你在遗留 Scala 和 Java 代码以及第3方库中无缝使用 ZIO 的所有特性。

### 同步副作用

一个同步副作用可以使用 `ZIO.attempt` 转换为 ZIO Effect ：
```
import scala.io.StdIn

val readLine: Task[String] =
  ZIO.attempt(StdIn.readLine())
```

所产生 Effect 的错误类型总是 `Throwable`，因为副作用可能抛出具有 `Throwable` 类型的任何值的异常。

如果书籍给定副作用不会抛出任何异常，则副作用可以使用 `ZIO.succeed` 转换为 ZIO Effect ：
```
def printLine(line: String): UIO[Unit] =
  ZIO.succeed(println(line))
```

当使用 `ZIO.succeed` 时你应该小心——当不确定副作用是否总是产生时，首先 `ZIO.attempt` 来转换副作用。
```
import java.io.IOException

val readLine2: IO[IOException, String] =
  ZIO.attempt(StdIn.readLine()).refineToOrDie[IOException]
```

### 异步副作用

一个基于回调 API 的异步副作用可以使用 `ZIO.async` 转换为 ZIO Effect ：
```
object legacy {
  def login(
    onSuccess: User => Unit,
    onFailure: AuthError => Unit): Unit = ???
}

val login: IO[AuthError, User] =
  IO.async[AuthError, User] { callback =>
    legacy.login(
      user => callback(IO.succeed(user)),
      err  => callback(IO.fail(err))
    )
  }
```

异步 ZIO Effect 比基于回调的 API 更易于使用，而且它们受益于 ZIO 特性，如：中断、资源安全和卓越的错误处理。

## 阻塞性的同步副作用

一些副作用使用阻塞 IO，或以其它方式使用线程进入等待状态。如果不仔细管理，这些副作用会从你的应用程序主线程池中耗尽线程，导致工作饥饿。

ZIO 提供了 `zio.Blocking`，它可用安全地将阻塞副作用转换为 ZIO Effect 。

一个阻塞副作用可以使用 `attemptBlocking` 方法直接转换为阻塞的 ZIO Effect 。
```
val sleeping =
  ZIO.attemptBlocking(Thread.sleep(Long.MaxValue))
```

所产生 Effect 将在一个专门设计为用于阻塞 Effect 的单独线程池里执行。

使用 `attemptBlockingInterrupt` 方法调用 `Thread.interrupt` 可以中断副作用。 

一些阻塞副作用只能通过调用取消 Effect 来中断。你可以使用 `attemptBlockingCancelable` 方法转换这些副作用：
```
import java.net.ServerSocket
import zio.UIO

def accept(l: ServerSocket) =
  ZIO.attemptBlockingCancelable(l.accept())(UIO.succeed(l.close()))
```

如果副作用已经被转换为 ZIO Effect ，则可以使用 `blocking` 方法确保 Effect 在阻塞线程池上执行，而非使用 `attemptBlocking`：
```
import scala.io.{ Codec, Source }

def download(url: String) =
  Task.attempt {
    Source.fromURL(url)(Codec.UTF8).mkString
  }

def safeDownload(url: String) =
  ZIO.blocking(download(url))
```

