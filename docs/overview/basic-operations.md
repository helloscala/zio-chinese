# 基本操作

*原文：[https://zio.dev/next/overview/overview_basic_operations](https://zio.dev/next/overview/overview_basic_operations)*

## Mapping

可以通过调用 `ZIO#map` 方法映射 Effect 的成功通道。这可以让你改变 Effect 的成功值。
```scala
import zio._

val succeeded: UIO[Int] = IO.succeed(21).map(_ * 2)
```

可以通过调用 `ZIO#mapError` 方法映射 Effect 的错误通道。这可以让你必应 Effect 的失败值。
```scala
val failed: IO[Exception, Unit] = 
  IO.fail("No no!").mapError(msg => new Exception(msg))
```

注意，映射 Effect 的成功或错误通道并不会改变 Effect 的成功或失败，就像相同的方式映射 Either 不会改变 `Either` 要么是 `Left` 要么是 `Right` 一样。

## Chaining

你可以使用 `flatMap` 方法按顺序执行两个 Effect，它要求你传递一个回调，回调将接收第一个 Effect 的值（结果），并且返回依赖于此值的第二个 Effect：
```scala
val sequenced = 
  readLine.flatMap(input => printLine(s"You entered: $input"))
```

如果第一个 Effect 失败，传入 `flatMap` 的回调将永远不会被调用，并且通过 `flatMap` 组合后的 Effect 也将返回该失败。

在任何 Effect 链中，第一次失败都会短路整个链路，就像抛出异常会过早地退出一系列语句一样。

## For Comprehensions

因为 `ZIO` 数据类型同时支持 `flatMap` 和 `map`，你可以使用 Scala 的 `for 推导式(for coprehensions)` 构建顺序 Effect：
```scala
val program = 
  for {
    _    <- printLine("Hello! What is your name?")
    name <- readLine
    _    <- printLine(s"Hello, ${name}, welcome to ZIO!")
  } yield ()
```

For 推导式为组合 Effect 链路提供了更具过程式的语法。

## Zipping

你可以使用 `ZIO#zip` 方法组合两个 Effect 为单个 Effect。所生成 Effect 的成功组将使用一个元组包含两个 Effect 的成功值：
```scala
val zipped: UIO[(String, Int)] = 
  ZIO.succeed("4").zip(ZIO.succeed(2))
```

注意 `zip` 按顺序操作：左侧 Effect 将在右侧 Effect 之前执行。

在任何 `zip` 操作中，如果左侧或右侧失败，则组合后的 Effect 也将失败，因为构造元组需要两个值。

有时，当 Effect 的成功值无用时（例如，它是 `Unit`），使用 `ZIO#zipLeft` 或 `ZIO#zipRight` 函数会更方便，它首先执行 `zip`，然后映射到元组上，并丢弃一边或另一边：
```scala
val zipRight1 = 
  printLine("What is your name?").zipRight(readLine)
```

`zipRight` 和 `zipLeft` 函数有符号别名，分别称为 `*>` 和 `<*`。一些开发人员发现这些操作更容易阅读：
```scala
val zipRight2 = 
  printLine("What is your name?") *>
  readLine
```

