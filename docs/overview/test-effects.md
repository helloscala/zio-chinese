# 测试 Effect

测试函数式 Effect 有许多方法，包括使用 free monads、tagless-final 和带环境的 Effect。虽然这些所有方法都与 ZIO 兼容，但是最简单和符合人类直观的是 *带环境的 Effect*。

本节介绍带环境的 Effect，并向你展示如何使用它们编写可测试的函数式代码。

## 环境


