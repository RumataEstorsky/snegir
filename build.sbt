name := """snegir-web"""
organization := "valerii.svechikhin"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.2"

libraryDependencies ++= Seq(
  guice,
  "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0" % Test,
  "mysql" % "mysql-connector-java" % "8.0.20",
  "com.typesafe.play" %% "play-slick" % "5.0.0",
  "com.typesafe.play" %% "play-slick-evolutions" % "5.0.0",
)

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "valerii.svechikhin.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "valerii.svechikhin.binders._"
