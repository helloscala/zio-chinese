# 并发基础

ZIO 对使用纤程（*fiber*）的并发性具有低级支持。虽然纤程非常强大，但它们是很低级的工具。为了提高生产力，ZIO 提供了基于纤程的高级操作。

可能的话你应该总是使用高级操作，而不是直接使用纤程。为了完整起见，本节介绍了纤程和一些基于它们的高级操作。

## 纤程

ZIO 的并发性建立在纤程之上，纤程是由 ZIO 运行时系统实现的轻量级“绿色线程”。

与操作系统线程不同，纤程几乎不消耗内存，具有可生缩的堆栈，不会浪费资源阻塞（当前线程），如果它们暂停且不可达将自动进行垃圾回收。

纤程由 ZIO 运行时调度，并将相互合作，即使在单线程环境中操作（如：JavaScript，甚至在配置一个线程时的JVM）中也能实现多任务处理。

ZIO 中的所有 Effect 都由一些纤程来执行。如果你没有创建纤程，则它由你正在使用的一些操作（若操作是并发或并行的）或 ZIO 运行时系统创建。

即使你只写“单线程”代码，无并行或并发操作，仍然会存在至少一个纤程：执行 Effect 的“main”纤程。

### 纤程数据类型

每个 ZIO 纤程都负责执行一些 Effect，ZIO 中的 `Fiber` 数据类型表示运行计算的一个“句柄”（handle）。`Fiber` 数据类型类似于 Scala 中的 `Future` 数据类型。

ZIO 中的 `Fiber[E, A]` 数据类型有两个类型参数：

- **`E` 失败类型**。纤程可能以该类型的值失败。
- **`A` 成功类型**。纤程可能以该类型的值成功。

纤程没有 `R` 类型参数，因为它对已经运行的 Effect 建模，而（Effect）已经向它们提供了所需的环境。

### 分叉 Effect

创建一个纤程最基础的方法是从一个已存在的 Effect 分叉（fork）。从概念上讲，分叉 Effect 将在一个新的纤程上执行，并给你一个新创建 `Fiber` 的引用。

以下代码创建单个纤程执行 `fib(100)`：

```scala
def fib(n: Long): UIO[Long] = UIO {
  if (n <= 1) UIO.succeed(n)
  else fib(n - 1).zipWith(fib(n - 2))(_ + _)
}.flatten

val fib100Fiber: UIO[Fiber[Nothing, Long]] = 
  for {
    fiber <- fib(100).fork
  } yield fiber
```

### 连接（Joining）纤程

`Fiber` 上的一个方法 `Fiber#join`，它返回一个 Effect。`Fiber#join` 返回的 Effect 将根据纤程的（运行）情况成功或失败：

```scala
for {
  fiber   <- IO.succeed("Hi!").fork
  message <- fiber.join
} yield message
```

### 等待纤程

`Fiber` 上的另一个方法 `Fiber#await`，它返回包含 `Exit` 值的 Effect，提供纤程如何完成的完整信息。

```scala
for {
  fiber <- IO.succeed("Hi!").fork
  exit  <- fiber.await
} yield exit
```

## 中断纤程

不需要结果的纤程可以被中断，这将立即终止纤程，安全的释放所有资源并运行所有终结器。

像 `await` 一样，`Fiber#interrupt` 返回一个 `Exit` 描述纤程怎样完成的。

```scala
for {
  fiber <- IO.succeed("Hi!").forever.fork
  exit  <- fiber.interrupt
} yield exit
```

根据设计，通过 `Fiber#interrupt` 返回的 Effect 直到纤程完成前都不能恢复。如果此行为不是想要的，你可以 `fork` 中断本身：

```scala
for {
  fiber <- IO.succeed("Hi!").forever.fork
  _     <- fiber.interrupt.fork // I don't care!
} yield ()
```

### 组合纤程

ZIO 使用 `Fiber#zip` 或 `Fiber#zipWith` 组合纤程。

这些方法合并两个纤程到一个。如果其中之一失败，则组合后的纤程也将失败。

```scala
for {
  fiber1 <- IO.succeed("Hi!").fork
  fiber2 <- IO.succeed("Bye!").fork
  fiber   = fiber1.zip(fiber2)
  tuple  <- fiber.join
} yield tuple
```

纤程的另一种组合方式是使用 `Fiber#orElse`。如果第一个纤程成功，组合后的纤程将以此结果成功；否则，组合后的纤程将使用第二个纤程的退出值完成（无论成功还是失败）。

```scala
for {
  fiber1 <- IO.fail("Uh oh!").fork
  fiber2 <- IO.succeed("Hurray!").fork
  fiber   = fiber1.orElse(fiber2)
  message  <- fiber.join
} yield message
```

## 并行

ZIO 提供提供许多用于并行执行 Effect 的操作。这些方法都是以 `Par` 后缀命名，它可以帮助你识别并行化的代码。

例如，原始版本的 `ZIO#zip` 方法串行的合并两个 Effect 到一起。但是 `ZIO#zipPar` 方法并行的将两个 Effect 合并到一起。

下表总结了一些串行操作和相关的并行版本：

| 描述 | 串行操作 | 并行版本 |
| ---- | --------- | ------ |
| 将两个 Effect 合成一个 | `ZIO#zip` | `ZIO#zipPar` |
| 将两个 Effect 合成一个 | `ZIO#zipWith` | `ZIO#zipWithPar` |
| 将多个 Effect 合成一个 | `ZIO#tupled` | `ZIO#tupledPar` |
| 从多个 Effect 收集 | `ZIO.collectAll` | `ZIO.collectAllPar` |
| 高效的循环处理 | `ZIO.foreach` | `ZIO.foreachPar` |
| 减少（reduce）多个值 | `ZIO.reduceAll` | `ZIO.reduceAllPar` |
| 合并多个值 | `ZIO.mergeAll` | `ZIO.mergeAllPar` |

对于所有并行操作，如果一个 Effect 失败，则其它都将被中断，以尽量减少不必要的计算。

如果快速失败不是想要的行为，可以先用 `ZIO#either` 或 `ZIO#option` 方法将可能失败的 Effect 转换为无懈可击的 Effect（注：使用 `Either` 或 `Option` 包装的以成功值完成的 Effect）。

## 竞争

ZIO 允许并行竞争多个 Effect，返回第一个成功完成的结果：

```scala
for {
  winner <- IO.succeed("Hello").race(IO.succeed("Goodbye"))
} yield winner
```

如果你想要第一个成功或失败，而非第一个成功，那么你可以使用 `left.either race right.either`（注：将包含失败的 `IO[E, A]` 转换为只包含成功值的 `UIO[Either[E, A]]`）。

## 超时

ZIO 可以使用 `ZIO#timeout` 方法在任何 Effect 上超时，它将以 `Option` 作为成功返回一个新的 Effect。值为 `None` 表示在 Effect 完成之前超时。

```scala
IO.succeed("Hello").timeout(10.seconds)
```

如果一个 Effect 超时，那么它将被中断而非继续在后台执行，这样就没有资源被浪费。

