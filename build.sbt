name := "queuing-service"

version := "1.0"

scalaVersion := "2.12.4"

lazy val akkaHttpVersion = "10.0.11"
lazy val akkaVersion = "2.5.11"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream"  % akkaVersion,
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-xml"        % akkaHttpVersion,

  "org.scalatest" %% "scalatest" % "3.0.1" % Test,
  "com.github.tomakehurst" % "wiremock" % "2.12.0" % Test
)
