name := "queuing-service"

version := "1.0"

scalaVersion := "2.12.2"

lazy val akkaVersion = "2.5.3"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion,

  "org.scalatest" %% "scalatest" % "3.0.1" % Test,
  "com.github.tomakehurst" % "wiremock" % "2.12.0" % Test
)
