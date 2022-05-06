ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.8"

lazy val root = (project in file("."))
  .settings(
    name := "akka-essentials",
    idePackagePrefix := Some("org.abteilung6.akka")
  )

val akkaVersion = "2.6.19"
val logbackVersion = "1.2.11"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
  "ch.qos.logback" % "logback-classic" % logbackVersion,
)
