ThisBuild / scalaVersion := "2.13.8"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.example"
ThisBuild / organizationName := "example"

val ZIO_VERSION = "2.0.0-RC3"

lazy val root = (project in file("."))
  .settings(
    name := "zio-chinese",
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % ZIO_VERSION,
      "dev.zio" %% "zio-test" % ZIO_VERSION % Test
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )
