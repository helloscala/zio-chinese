# 摘要

*原文：[https://zio.dev/next/overview/](https://zio.dev/next/overview/)*

ZIO 是一个基于纯函数式编程的异步和并发编程库。

> 有关纯函数工编程怎样处理 Effect ，如：输入和输出，见 [背景](background.md) 章节。

`ZIO` 是 ZIO 的核心，一个受 Haskell 的 `IO` 单子启发的强大 Effect 类型。

## ZIO

`ZIO[R, E, A]` 数据类型有3个类型参数：
- **`R` - 环境类型**。 Effect 需要一个 `R` 类型的环境。如果该类型参数为 `Any`，意味着 Effect 不需要环境，因此你可以使用任何值运行 Effect （例如：`()` 值）。
- **`E` - 故障类型**。 Effect 也许以 `E` 类型的值失败。有些程序会使用 `Throwable` 作为 `E`。如果该类型参数为 `Nothing`，意味着 Effect 不可能失败，因为不存在类型为 `Nothing` 的值。
- **`A` - 成功类型**。 Effect 也许以 `A` 类型的值成功。如果该参数类型为 `Unit`，意味着不产生有用的信息，而如果类型为 `Nothing`，则表示 Effect 将永远运行（或直到失败）。

例如： Effect 类型 `ZIO[Any, IOException, Byte]` 不需要环境，也许以 `IOException` 类型失败，或者以 `Byte` 类型成功。

类型ZIO[R，E，A]的值类似于以下函数类型的有效版本：
```scala
R => Either[E, A]
```

这个函数需要一个 `R`，可能产生代码失败的 `E` 或者代表成功的 `A`。当然，ZIO Effect 实际上不是函数，因为它们模拟了复杂的 Effect ，如异步和并发 Effect 。

## 类型别名

`ZIO` 数据类型是 ZIO 中唯一的 Effect 类型。然而，有一系列类型别名和配套对象可以简化常见情况：
- `UIO[A]` —— 是 `ZIO[Any, Nothing, A]` 的类型别名，表示不需要环境，也不可能失败，但可以以 `A` 成功的 Effect 。
- `URIO[R, A]` —— 是 `ZIO[R, Nothing, A]` 的类型别名，表示需要环境，不可能失败，但可以以 `A` 成功的 Effect 。
- `Task[A]` —— 是 `ZIO[Any, Throwable, A]` 的类型别名，表示不需要环境，也许以 `Throwable` 失败，或者以 `A` 成功的 Effect 。
- `RIO[R, A] —— 是 `ZIO[R, THrowable, A]` 的类型别名，表示需要环境，也许以 `Throwable` 失败，或者以 `A` 成功的 Effect 。
- `IO[E, A]` —— 是 `ZIO[Any, E, A]` 的类型别名，表示不需要环境，也许以 `E` 失败，或者以 `A` 成功的 Effect 。

这些类型别名都有伴身对象，这些伴随对象都有可用于构建适当类型的值的方法。

如果你是函数式 Effect 的新手，我们建议从 `Task` 类型开始，它有单个类型参数，并且最近似于 Scala 标准库内建的 `Future` 类型。

如果你正在使用 ***Cats Effect*** 库，你可能发现 `RIO` 类型很有用，因为它允许你通过第3方库或你的应用程序提供线程环境。

无论你在应用程序中使用哪种类型的别名，`UIO` 都可用于描述万无一失的 Effect ，包括处理所有错误所产生的 Effect 。

最后，如果您是经验丰富的函数式程序员。尽管你可能会发现在应用程序的不同部分创建自己的系列类型别名很有用，还是建议直接使用 `ZIO` 数据类型，

