name := "SpideoExam"

version := "1.0"

scalaVersion := "2.12.7"
val akkaVersion = "2.5.18"
val akkaHTTPVersion = "10.1.5"
val scalatestVersion = "3.0.5"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor"             % akkaVersion,
  "com.typesafe.akka" %% "akka-stream"            % akkaVersion,
  "com.typesafe.akka" %% "akka-http"              % akkaHTTPVersion,
  "com.typesafe.akka" %% "akka-http-spray-json"   % akkaHTTPVersion,

  "com.typesafe.akka" %% "akka-testkit"           % akkaVersion % Test,
  "com.typesafe.akka" %% "akka-stream-testkit"    % akkaVersion % Test,
  "com.typesafe.akka" %% "akka-http-testkit"      % akkaHTTPVersion % Test,

  "org.scalatest"     %% "scalatest"              % scalatestVersion % Test
)