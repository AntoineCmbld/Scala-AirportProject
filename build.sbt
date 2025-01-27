
ThisBuild / scalaVersion     := "3.6.2"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.airport"
ThisBuild / organizationName := "airport"

lazy val root = (project in file("."))
  .settings(
    name := "ScalaAirportProject",
    Compile / run / fork := true,
    Compile / run / connectInput := true
  )

libraryDependencies ++= Seq(
  "org.tpolecat" %% "skunk-core" % "1.1.0-M3",
  "org.typelevel" %% "cats-effect" % "3.6-623178c",
  "org.http4s" %% "http4s-blaze-server" % "1.0.0-M41",
  "org.http4s" %% "http4s-dsl" % "1.0.0-M44",
  "org.http4s" %% "http4s-circe" % "1.0.0-M44",
  "org.http4s" %% "http4s-jawn" % "1.0.0-M44",
  "io.circe" %% "circe-generic" % "latest.integration",
  "org.typelevel" %% "log4cats-slf4j" % "2.7.0",
  "org.slf4j" % "slf4j-simple" % "1.7.32",
)


Compile / run / fork := true
Compile / run / connectInput := true

// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.
