# 处理资源

*原文：[https://zio.dev/next/overview/overview_handling_resources](https://zio.dev/next/overview/overview_handling_resources)*

本节介绍一些在使用 ZIO 完全处理资源的常见方法。

ZIO 的资源管理特性跨同步、异步、并发和其它 Effect 类型工作，即使在应用程序中存在故障、中断或缺陷的情况下也能提供强有力的保证。

## Finalizing

ZIO 使用 `ZIO#ensuring` 方法提供类型 `try` / `finally` 的功能。

像 `try` / `finally`，`ensuring` 操作保证如果一个 Effect 开始执行，然后终止（对于任何原因），那么终结器（finalizer）都将执行。

```scala
val finalizer = 
  UIO.succeed(println("Finalizing!"))
// finalizer: UIO[Unit] = zio.ZIO$Succeed@44970831

val finalized: IO[String, Unit] = 
  IO.fail("Failed!").ensuring(finalizer)
// finalized: IO[String, Unit] = zio.ZIO$Ensuring@282ca4e3
```

终结器不允许失败，这意味着它必须在内部处理任何错误。

像 `try` / `finally`，终结器不能嵌套，任何内部终结器的失败都不会影响外部终结器。嵌套终结器将按相反的顺序线性（非并行）执行。

不像 `try` / `finally`，`ensuring` 适用于所有 Effect 类型，包括异步和并发 Effect。

## 获取 释放（Acquire Release）

`try` / `finally` 的一个常见用途是安全的获取和释放资源，类似新的套接字连接或打开文件：

```scala
val handle = openFile(name)

try {
  processFile(handle)
} finally closeFile(handle)
```

ZIO 使用 `ZIO#acquireRelease` 封装了这种常见模式，允许你指定一个 *acquire* Effect 获取资源；一个 *release* Effect 释放资源；和一个 *use* Effect 来使用资源。

  即使在出现错误或中断的情况下，释放 Effect 也保证由运行时系统执行。

```scala
val groupedFileData: IO[IOException, Unit] = 
  openFile("data.json").acquireReleaseWith(closeFile(_)) { file =>
    for {
      data    <- decodeData(file)
      grouped <- groupData(data)
    } yield grouped
  }
```

像 `ensuring`，`acquireRelease` 具有组合语义，因此，如果一个 `acquireRelease` 嵌套在另一个获取释放中，并且获得外部资源，那么即使内部释放失败，也将始终调用外部释放。

