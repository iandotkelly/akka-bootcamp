lazy val akkaHttpVersion = "10.0.9"
lazy val akkaVersion    = "2.5.3"

//javaOptions += "-Djava.library.path=\"/Users/ian/.ivy2/cache/org.knowhowlab.osgi/sigar/bundles/\""

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization    := "com.example",
      scalaVersion    := "2.12.2"
    )),
    name := "bootcamp",
    libraryDependencies ++= Seq(
      "com.typesafe.akka"          %% "akka-http"                 % akkaHttpVersion,
      "com.typesafe.akka"          %% "akka-http-xml"             % akkaHttpVersion,
      "com.typesafe.akka"          %% "akka-http-spray-json"      % akkaHttpVersion,
      "com.typesafe.akka"          %% "akka-stream"               % akkaVersion,
      "com.datastax.cassandra"     %  "cassandra-driver-core"     % "3.3.0",
      //"org.apache.cassandra"       %  "cassandra-all"             % "3.11.0",
      //"org.knowhowlab.osgi"        %  "sigar"                     % "1.6.5_01",
      "info.archinnov" % "achilles-embedded" % "5.2.1",
      "com.typesafe.akka"          %% "akka-http-testkit"         % akkaHttpVersion % Test,
      "org.scalatest"              %% "scalatest"                 % "3.0.1"         % Test
    )
  )
