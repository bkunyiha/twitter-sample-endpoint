
val algebirdVersion = "0.13.4"
val circeVersion = "0.9.3"
val emojiJavaVersion = "4.0.0"
val http4sVersion = "0.18.20"
val http4sRhoVersion = "0.18.0"
val LogbackVersion = "1.2.3"
val pureConfigVersion = "0.10.0"
val Specs2Version = "4.1.0"

lazy val root = (project in file("."))
  .enablePlugins(BuildInfoPlugin)
  .settings(
    organization := "com.banno",
    name := "twitter-sample-endpoint",
    description := "Analytics for Titter sample endpoint API",
    version := "0.0.1-SNAPSHOT",
    scalaVersion := "2.12.6",
    scalacOptions ++= Seq(
      "-Xexperimental",
      "-deprecation",
      "-encoding", "UTF-8",
      "-feature",
      "-language:existentials",
      "-language:higherKinds",
      "-language:implicitConversions",
      "-unchecked",
      "-language:postfixOps",
      "-Xlint:inaccessible",
      "-Xlint:unsound-match",
      "-Xfatal-warnings",
      "-Xlint",
      "-Yno-adapted-args",
      "-Ywarn-dead-code",
      "-Ywarn-numeric-widen",
      "-Ywarn-value-discard",
      "-Ywarn-unused",
      "-Xfuture"
    ),
    scalacOptions in (Compile, console) --= Seq("-Ywarn-unused:imports", "-Xfatal-warnings"),
    libraryDependencies ++= Seq(
      "org.http4s"            %% "http4s-blaze-server" % http4sVersion,
      "org.http4s"            %% "http4s-blaze-client" % http4sVersion,
      "org.http4s"            %% "http4s-circe"        % http4sVersion,
      "org.http4s"            %% "http4s-dsl"          % http4sVersion,
      "org.http4s"            %% "http4s-circe"        % http4sVersion,
      "org.http4s"            %% "rho-swagger"         % http4sRhoVersion,
      "io.circe"              %% "circe-generic"       % circeVersion,
      "com.twitter"           %% "algebird-core"       % algebirdVersion,
      "com.vdurmont"          %  "emoji-java"          % emojiJavaVersion,
      "com.github.pureconfig" %% "pureconfig"          % pureConfigVersion,
      "org.specs2"            %% "specs2-core"         % Specs2Version % "test",
      "ch.qos.logback"        %  "logback-classic"     % LogbackVersion
    ),
    addCompilerPlugin("org.spire-math" %% "kind-projector"     % "0.9.6"),
    addCompilerPlugin("com.olegpy"     %% "better-monadic-for" % "0.2.4")
  )
  .settings(
    //Build info
    buildInfoObject := "SbtBuildInfo",
    buildInfoPackage := "com.banno.twittersampleendpoint",
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion, description),
    buildInfoOptions += BuildInfoOption.BuildTime
  )