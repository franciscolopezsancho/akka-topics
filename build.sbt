val AkkaVersion = "2.6.10"
val logbackVersion = "1.2.3"

lazy val `up-and-running` = project
  .in(file("up-and-running"))
  .settings(
    scalaVersion := "2.13.1",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
      "ch.qos.logback" % "logback-classic" % logbackVersion,
      )
    )
lazy val `unit-testing` = project
  .in(file("unit-testing"))
  .settings(
    scalaVersion := "2.13.1",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
      "ch.qos.logback" % "logback-classic" % logbackVersion,
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % AkkaVersion % Test,
      "org.scalatest" %% "scalatest" % "3.1.4" % Test,
    ))

ThisBuild / watchTriggeredMessage := Watch.clearScreenOnTrigger