val algebirdVersion = "0.13.4"
val circeVersion = "0.9.3"
val emojiJava = "4.0.0"
val http4sVersion = "0.18.20"
val LogbackVersion = "1.2.3"
val Specs2Version = "4.1.0"

lazy val root = (project in file("."))
  .settings(
    organization := "com.banno",
    name := "twitter-sample-endpoint",
    version := "0.0.1-SNAPSHOT",
    scalaVersion := "2.12.6",
    libraryDependencies ++= Seq(
      "org.http4s"          %% "http4s-blaze-server" % http4sVersion,
      "org.http4s"          %% "http4s-blaze-client" % http4sVersion,
      "org.http4s"          %% "http4s-circe"        % http4sVersion,
      "org.http4s"          %% "http4s-dsl"          % http4sVersion,
      "org.http4s"          %% "http4s-circe"        % http4sVersion,
      "io.circe"            %% "circe-generic"       % circeVersion,
      "com.twitter"         %% "algebird-core"       % algebirdVersion,
      "com.vdurmont"        %  "emoji-java"          % emojiJava,
      "org.specs2"          %% "specs2-core"         % Specs2Version % "test",
      "ch.qos.logback"      %  "logback-classic"     % LogbackVersion
    ),
    addCompilerPlugin("org.spire-math" %% "kind-projector"     % "0.9.6"),
    addCompilerPlugin("com.olegpy"     %% "better-monadic-for" % "0.2.4")
  )

