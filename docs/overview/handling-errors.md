# 处理错误

*原文：[https://zio.dev/next/overview/overview_handling_errors](https://zio.dev/next/overview/overview_handling_errors)*

本节介绍一些检测和应对失败的常用方法。

## Either

你可以使用 `ZIO#either` 体现失败，它接收一个 `ZIO[R, E, A]` 并生成一个 `ZIO[R, Nothing, Either[E, A]]`：
```
val zeither: UIO[Either[String, Int]] = 
  IO.fail("Uh oh!").either
```

你可以使用 `ZIO.absolve` 淹没失败，它与 `either` 相反，它会将 `ZIO[R, Nothing, Either[E, A]]` 变成 `ZIO[R, E, A]`：
```
def sqrt(io: UIO[Double]): IO[String, Double] =
  ZIO.absolve(
    io.map(value =>
      if (value < 0.0) Left("Value must be >= 0.0")
      else Right(Math.sqrt(value))
    )
  )
```

## 捕获所有错误

如果你想捕获所有错误类型并有效地尝试恢复，你可以使用 `catchAll` 方法：
```
val z: IO[IOException, Array[Byte]] = 
  openFile("primary.json").catchAll(_ => 
    openFile("backup.json"))
```

在传递给 `catchAll` 的回调中，你可以返回一个具有不同错误类型的 Effect（或许是 `Nothing`），这将反映在 `catchAll` 返回的 Effect 类型中。

## 捕获部分错误

如果你想只捕获部分异常类型并有效地尝试恢复，你可以使用 `catchSome` 方法：
```
val data: IO[IOException, Array[Byte]] = 
  openFile("primary.data").catchSome {
    case _ : FileNotFoundException => 
      openFile("backup.data")
  }
```

不像 `catchAll`，`catchSome` 不能减少或消除错误类型，尽管它可以将错误扩大到更广泛的错误类型。

## Fallback（后退）

你可以在一个 Effect 错误时尝试另一个 Effect，使用 `orElse` 结合两个 Effect：
```
val primaryOrBackupData: IO[IOException, Array[Byte]] = 
  openFile("primary.data").orElse(openFile("backup.data"))
```

## Folding（折叠）

Scala 的 `Option` 和 `Either` 数据类型有 `fold`，它让你同时处理失败和成功。以类似的方式，`ZIO` Effect 也有几个方法允许你同时处理失败和成功。

第一个折叠方法 `fold`，让你非有效的（注：non-effectfully，指返回的值是有副作用的）处理失败和成功，为每种情况提供一个非有效的处理程序：
```
lazy val DefaultData: Array[Byte] = Array(0, 0)

val primaryOrDefaultData: UIO[Array[Byte]] = 
  openFile("primary.data").fold(
    _    => DefaultData,
    data => data)
```

第二个折叠方法 `foldZIO`，让你有效的（注：effectfully，无副作用的意思）处理失败和成功，为每种情况提供一个有效的（但依然是纯函数的）处理程序：
```
val primaryOrSecondaryData: IO[IOException, Array[Byte]] = 
  openFile("primary.data").foldZIO(
    _    => openFile("secondary.data"),
    data => ZIO.succeed(data))
```

几乎所有的错误处理方法都是用 `foldZIO` 定义的，因为它既强大又快速：
```
val urls: UIO[Content] =
  readUrls("urls.json").foldZIO(
    error   => IO.succeed(NoContent(error)), 
    success => fetchContent(success)
  )
```

## 重试

ZIO 数据类型上有大量有用方法用于重试失败的 Effect。

其中最基本的是 `ZIO#retry`，它接收一个 `Schedule` 并返回一个新的 Effect。如果失败，它将依照指定的策略重试第一个 Effect：
```
val retriedOpenFile: ZIO[Clock, IOException, Array[Byte]] = 
  openFile("primary.data").retry(Schedule.recurs(5))
```

下一个最强大的函数是 `ZIO#retryOrElse`。如果 Effect 在指定的策略中不成功，则允许使用回退值：
```
openFile("primary.data").retryOrElse(
  Schedule.recurs(5), 
  (_, _) => ZIO.succeed(DefaultData))
```

最后，`ZIO#retryOrElseEither` 方法允许在回退中返回一个不同的类型。

有关构建 Schedule 的更多信息，见 [Schedule](https://zio.dev/next/datatypes/misc/schedule/) 文档。

