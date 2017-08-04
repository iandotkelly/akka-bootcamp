lazy val akkaHttpVersion = "10.0.9"
lazy val akkaVersion    = "2.5.3"

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization    := "com.bitbrew",
      scalaVersion    := "2.12.2"
    )),
    name := "bootcamp",
    libraryDependencies ++= Seq(
      "com.typesafe.akka"          %% "akka-http"                 % akkaHttpVersion,
      "com.typesafe.akka"          %% "akka-http-xml"             % akkaHttpVersion,
      "com.typesafe.akka"          %% "akka-http-spray-json"      % akkaHttpVersion,
      "com.typesafe.akka"          %% "akka-stream"               % akkaVersion,
      "com.datastax.cassandra"     %  "cassandra-driver-core"     % "3.3.0",
      "info.archinnov"             % "achilles-embedded"          % "5.2.1",
      "com.typesafe.akka"          %% "akka-http-testkit"         % akkaHttpVersion % Test,
      "org.scalatest"              %% "scalatest"                 % "3.0.1"         % Test,
      "log4j"                      % "log4j"                      % "1.2.17" % Test,
      "org.slf4j"                  % "slf4j-simple"               % "1.7.25" % Test
    )
  )
