# 测试 Effect

*原文：[https://zio.dev/next/overview/overview_testing_effects](https://zio.dev/next/overview/overview_testing_effects)*

测试函数式 Effect 有许多方法，包括使用 free monads、tagless-final 和带环境的 Effect。虽然这些所有方法都与 ZIO 兼容，但是最简单和符合人类直观的是 *带环境的 Effect*。

本节介绍带环境的 Effect，并向你展示如何使用它们编写可测试的函数式代码。

## 环境

ZIO 数据类型有一个 `R` 类型参数，它用于描述 Effect 所需的 *环境* 类型。

ZIO Effect 可以使用 `ZIO.environment` 访问环境，它可以提供以一个 `R` 类型的值来直接访问环境。

```scala
for {
  env <- ZIO.environment[Int]
  _   <- printLine(s"The value of the environment is: $env")
} yield env
```

环境不只像整数那样是一个原始值。它可以更复杂，就像 `trait` 或 `case class`。

当环境是带有字段的类型时，`ZIO.access` 方法可以在单个方法调用中访问环境的指定部分：

```scala
final case class Config(server: String, port: Int)

val configString: URIO[Config, String] = 
  for {
    server <- ZIO.service[Config].map(_.server)
    port   <- ZIO.service[Config].map(_.port)
  } yield s"Server: $server, port: $port"
```

甚至 Effect 自身也可以被存储在环境中！在这种情况下，为了访问并执行一个 Effect，可以使用 `ZIO.serviceWithZIO` 方法。

```scala
trait DatabaseOps {
  def getTableNames: Task[List[String]]
  def getColumnNames(table: String): Task[List[String]]
}

val tablesAndColumns: ZIO[DatabaseOps, Throwable, (List[String], List[String])] = 
  for {
    tables  <- ZIO.serviceWithZIO[DatabaseOps](_.getTableNames)
    columns <- ZIO.serviceWithZIO[DatabaseOps](_.getColumnNames("user_table"))
  } yield (tables, columns)
```

当从环境中访问 Effect 时，如上例所示，该 Effect 称为 *环境 Effect*。

稍后，我们将看到环境 Effect 是如何为测试 ZIO 应用程序提供一个简单方法的。

### 提供环境

如果不首先提供它们的环境，就不能运行需要环境的 Effect。

提供一个 Effect 所需环境的最简单方式是使用 `ZIO#provide` 方法：

```scala
val square: URIO[Int, Int] = 
  for {
    env <- ZIO.service[Int]
  } yield env * env

val result: UIO[Int] = square.provideEnvironment(ZEnvironment(42))
```

一旦为 Effect 提供了所需环境，那么你就会得到一个环境类型为 `Any` 的 Effect，表明它的要求已被满足。

`ZIO.environmentWithZIO` 和 `ZIO#provide` 的组合提供了充分利用环境 Effect 以便于测试所需的一切。

## 环境 Effect

环境 Effect 背后的基本理念是对 *接口* 进行 *编程*，而非 *实现*。在函数式 Scala 的场景中，接口不包含任何执行副作用的方法，尽管它们可以包含返回函数式 Effect 的方法。

我们使用 *ZIO 环境* 来完成繁重的工作，而不是在整个代码库中手动传递接口、不需要使用依赖注入来注入接口、也不需要使用不连贯的隐式对接口进行线程化，从而生成优雅、可推断且无痛的代码。

在本节中，我们将探讨如何通过开发一个可测试的数据库服务来使用环境 Effect。

### 定义服务

我们将在模块的帮助下定义数据库服务，模块接口只包含提供访问服务的单个字段。

```scala
object Database {
  trait Service {
    def lookup(id: UserID): Task[UserProfile]
    def update(id: UserID, profile: UserProfile): Task[Unit]
  }
}
trait Database {
  def database: Database.Service
}
```

在这个例子中，`Database` 是 *模块*，它包含 `Database.Service` *服务*。该服务只是位于模块的伴身对象内的一个普通接口，包含提供服务 *能力* 的函数。

### 提供助手（Provide Helpers）

为了使访问作为环境 Effect 的数据库服务更容易，我们将定义使用 `ZIO.serviceWithZIO` 的辅助函数。

```scala
object db {
  def lookup(id: UserID): RIO[Database, UserProfile] =
    ZIO.serviceWithZIO(_.database.lookup(id))

  def update(id: UserID, profile: UserProfile): RIO[Database, Unit] =
    ZIO.serviceWithZIO(_.database.update(id, profile))
}
```

这些辅助函数不是必需的，因为我们可以通过 `ZIO.serviceWithZIO` 直接访问数据库模块，但这些助手很容易编写并使代码（理解）更简单。

### 使用服务

现在，我们已定义好模块和辅助函数，已经准备好使用数据库服务构建示例：

```scala
val lookedupProfile: RIO[Database, UserProfile] = 
  for {
    profile <- db.lookup(userId)
  } yield profile
```

这个例子中的 Effect 仅通过环境与数据库交互，环境是提供访问数据库服务的模块。

为了实际上运行这样一个 Effect，我们需要提供一个数据库模块的实现。

### 实现 Live 服务

现在我们将实现一个 live 数据库模块，它将与我们的生产数据库进行实际交互。

```scala
trait DatabaseLive extends Database {
  def database: Database.Service = 
    new Database.Service {
      def lookup(id: UserID): Task[UserProfile] = ???
      def update(id: UserID, profile: UserProfile): Task[Unit] = ???
    }
}
object DatabaseLive extends DatabaseLive
```

在前面代码片段中，未提供两个数据库方法的实现，因为那将需要超出本教程范围的细节（注：访问某一具体数据库的代码实现逻辑，所以这里使用 `???` 代表未实现，但不影响整个教程的理解）。

### 运行数据库 Effect

我们现在有一个数据库模块，与数据库模块交互的辅助函数，以及一个数据库模块的 live 实现。

我们可以使用 `ZIO.provide` 向应用程序提供 live 数据库模块：

```scala
def main: RIO[Database, Unit] = ???

def main2: Task[Unit] = 
  main.provideEnvironment(ZEnvironment(DatabaseLive))
```

生成的 Effect 不需要环境，因此现在可以使用 ZIO 运行时执行。

### 实现测试服务

为了测试与数据库交互的代码，我们不想与真实的数据库进行交互，因为我们的测试很慢且不稳定，即使我们的应用逻辑是正确的也会随机失败。

尽管你可以使用 mock 库来创建测试模块，但在这里，我们将简单的直接创建测试模块来表明不涉及任何魔法（注：代码是可预测、易理解的）：

```scala
class TestService extends Database.Service {
  private var map: Map[UserID, UserProfile] = Map()

  def setTestData(map0: Map[UserID, UserProfile]): Task[Unit] = 
    Task { map = map0 }

  def getTestData: Task[Map[UserID, UserProfile]] = 
    Task(map)

  def lookup(id: UserID): Task[UserProfile] = 
    Task(map(id))

  def update(id: UserID, profile: UserProfile): Task[Unit] = 
    Task.attempt { map = map + (id -> profile) }
}
trait TestDatabase extends Database {
  val database: TestService = new TestService
}
object TestDatabase extends TestDatabase
```

因为这个模块只用于测试，它通过提取和更新一个硬编码 `Map` 中的数据来模拟与数据库的交互。为了使这个模块对纤程安全，你可以使用 `Ref` 而非 `var` 来持有 `map`。

### 测试数据库用例

为了测试需要数据库的代码，我们只需要提供我们的测试数据库模块。

```scala
def code: RIO[Database, Unit] = ???

def code2: Task[Unit] = 
  code.provideEnvironment(ZEnvironment(TestDatabase))
```

我们的应用代码可以与生产数据库模块或测试数据库模块一起工作。
