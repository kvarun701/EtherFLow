// build.sbt — Scala EtherFlow client
ThisBuild / scalaVersion := "3.3.1"
ThisBuild / version      := "1.0.0"
ThisBuild / organization := "io.github.kvarun701"

lazy val root = (project in file("."))
  .settings(
    name := "etherflow-scala",
    libraryDependencies ++= Seq(
      // sttp HTTP client
      "com.softwaremill.sttp.client4" %% "core"          % "4.0.0-M15",
      "com.softwaremill.sttp.client4" %% "circe"         % "4.0.0-M15",

      // circe JSON
      "io.circe" %% "circe-core"    % "0.14.6",
      "io.circe" %% "circe-generic" % "0.14.6",
      "io.circe" %% "circe-parser"  % "0.14.6",

      // http4s server (optional)
      "org.http4s" %% "http4s-ember-server" % "0.23.25",
      "org.http4s" %% "http4s-dsl"          % "0.23.25",
      "org.http4s" %% "http4s-circe"        % "0.23.25",

      // Test
      "org.scalatest" %% "scalatest" % "3.2.17" % Test
    ),
    scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked")
  )
