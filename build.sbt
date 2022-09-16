val AkkaVersion = "2.6.20"
val LogbackVersion = "1.2.3"
val ScalaVersion = "2.13.1"
val AkkaManagementVersion = "1.1.0"
val AkkaProjectionVersion = "1.2.2"
val ScalikeJdbcVersion = "3.5.0"
val AkkaHttpVersion = "10.2.4"
val AkkaGRPC = "2.0.0"
val ScalaTest = "3.1.4"
val JacksonVersion = "2.11.4" 

lazy val chapter02 = project
  .in(file("chapter02"))
  .settings(
    scalaVersion := ScalaVersion,
    scalafmtOnCompile := true,
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
      "com.typesafe.akka" %% "akka-actor" % AkkaVersion,
      "ch.qos.logback" % "logback-classic" % LogbackVersion,
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % AkkaVersion % Test,
      "org.scalatest" %% "scalatest" % ScalaTest % Test,
      )
    )

lazy val chapter03 = project
  .in(file("chapter03"))
  .settings(
    scalaVersion := ScalaVersion,
    scalafmtOnCompile := true,
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
      "com.typesafe.akka" %% "akka-actor" % AkkaVersion,
      "ch.qos.logback" % "logback-classic" % LogbackVersion,
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % AkkaVersion % Test,
      "org.scalatest" %% "scalatest" % ScalaTest % Test,
      )
    )

lazy val chapter04 = project
  .in(file("chapter04"))
  .settings(
    scalaVersion := ScalaVersion,
        scalafmtOnCompile := true,
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
      "ch.qos.logback" % "logback-classic" % LogbackVersion,
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % AkkaVersion % Test,
      "org.scalatest" %% "scalatest" % ScalaTest % Test,
    ))

lazy val chapter05 = project
  .in(file("chapter05"))
  .settings(
    scalaVersion := ScalaVersion,
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
      "ch.qos.logback" % "logback-classic" % LogbackVersion,
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % AkkaVersion % Test,
      "org.scalatest" %% "scalatest" % ScalaTest % Test,
    ))

lazy val chapter06 = project
  .in(file("chapter06"))
  .settings(
    scalaVersion := ScalaVersion,
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
      "ch.qos.logback" % "logback-classic" % LogbackVersion,
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % AkkaVersion % Test,
      "org.scalatest" %% "scalatest" % ScalaTest % Test,
    ))

lazy val `clustering-primitives` = project
  .in(file("clustering-primitives"))
  .settings(
    scalaVersion := ScalaVersion,
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-cluster-typed" % AkkaVersion,         
      "ch.qos.logback" % "logback-classic" % LogbackVersion,
      "com.lightbend.akka.management" %% "akka-management" % AkkaManagementVersion,
      "com.lightbend.akka.management" %% "akka-management-cluster-http" % AkkaManagementVersion,
      "com.typesafe.akka" %% "akka-cluster-sharding" % AkkaVersion,
      "com.typesafe.akka" %% "akka-discovery" % AkkaVersion

    ),
  )

lazy val clustering = project
  .in(file("clustering"))
  .settings(
    scalaVersion := ScalaVersion,
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
      "com.typesafe.akka" %% "akka-cluster-typed" % AkkaVersion,         
      "com.typesafe.akka" %% "akka-serialization-jackson" % AkkaVersion,
      "ch.qos.logback" % "logback-classic" % LogbackVersion,
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % AkkaVersion % Test,
      "com.lightbend.akka.management" %% "akka-management" % AkkaManagementVersion,
      "com.lightbend.akka.management" %% "akka-management-cluster-http" % AkkaManagementVersion,
    "com.typesafe.akka" %% "akka-cluster-sharding" % AkkaVersion,
      "com.typesafe.akka" %% "akka-discovery" % AkkaVersion, 
           "org.scalatest" %% "scalatest" % ScalaTest % Test,
    ),
  )

lazy val sharding = project
  .in(file("sharding"))
  .settings(
    scalaVersion := ScalaVersion,
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
      "com.typesafe.akka" %% "akka-cluster-typed" % AkkaVersion,         
      "com.typesafe.akka" %% "akka-cluster-sharding-typed" % AkkaVersion,         
      "com.typesafe.akka" %% "akka-serialization-jackson" % AkkaVersion,
      "ch.qos.logback" % "logback-classic" % LogbackVersion,
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % AkkaVersion % Test,
      "com.lightbend.akka.management" %% "akka-management" % AkkaManagementVersion,
      "com.lightbend.akka.management" %% "akka-management-cluster-http" % AkkaManagementVersion,
      "com.typesafe.akka" %% "akka-cluster-sharding" % AkkaVersion,
      "com.typesafe.akka" %% "akka-discovery" % AkkaVersion,
      "org.scalatest" %% "scalatest" % ScalaTest % Test,
    )
  )

lazy val persistence = project
  .in(file("persistence"))
  .settings(
    scalaVersion := ScalaVersion,
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
      "com.typesafe.akka" %% "akka-cluster-typed" % AkkaVersion,         
      "com.typesafe.akka" %% "akka-cluster-sharding-typed" % AkkaVersion,         
      "com.typesafe.akka" %% "akka-serialization-jackson" % AkkaVersion,
      "com.typesafe.akka" %% "akka-persistence-typed" % AkkaVersion,
      "com.typesafe.akka" %% "akka-persistence-testkit" % AkkaVersion % Test,
      "com.lightbend.akka" %% "akka-persistence-jdbc" % "5.0.0",
      "ch.qos.logback" % "logback-classic" % LogbackVersion,
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % AkkaVersion % Test,
      "com.lightbend.akka.management" %% "akka-management" % AkkaManagementVersion,
      "com.lightbend.akka.management" %% "akka-management-cluster-http" % AkkaManagementVersion,
      "com.typesafe.akka" %% "akka-cluster-sharding" % AkkaVersion,
      "com.typesafe.akka" %% "akka-discovery" % AkkaVersion,
      "org.scalatest" %% "scalatest" % ScalaTest % Test,
      "org.postgresql" % "postgresql" % "42.2.18",
    )
  )

lazy val projections = project
  .in(file("projections"))
  .dependsOn(persistence)
  .settings(
    scalaVersion := ScalaVersion,
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % AkkaVersion,
      "com.typesafe.akka" %% "akka-cluster-typed" % AkkaVersion,         
      "com.typesafe.akka" %% "akka-cluster-sharding-typed" % AkkaVersion,         
      "com.typesafe.akka" %% "akka-serialization-jackson" % AkkaVersion,
      "com.typesafe.akka" %% "akka-persistence-typed" % AkkaVersion,
      "com.typesafe.akka" %% "akka-persistence-testkit" % AkkaVersion % Test,
      "ch.qos.logback" % "logback-classic" % LogbackVersion,
      "com.lightbend.akka.management" %% "akka-management" % AkkaManagementVersion,
      "com.lightbend.akka.management" %% "akka-management-cluster-http" % AkkaManagementVersion,
      "com.typesafe.akka" %% "akka-cluster-sharding" % AkkaVersion,
      "com.lightbend.akka" %% "akka-projection-core" % AkkaProjectionVersion,
      "com.lightbend.akka" %% "akka-projection-eventsourced" % AkkaProjectionVersion,
      "com.lightbend.akka" %% "akka-projection-jdbc" % AkkaProjectionVersion,
      "com.lightbend.akka" %% "akka-projection-testkit" % AkkaProjectionVersion,
      "com.typesafe.akka" %% "akka-persistence-query" % AkkaVersion,
      "com.lightbend.akka" %% "akka-persistence-jdbc" % "5.0.0",
      "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
      "com.typesafe.akka" %% "akka-stream-testkit" % AkkaVersion ,
      "com.typesafe.akka" %% "akka-discovery" % AkkaVersion,
      "org.scalatest" %% "scalatest" % ScalaTest % Test,
      "org.scalikejdbc" %% "scalikejdbc"       % ScalikeJdbcVersion,
      "org.scalikejdbc" %% "scalikejdbc-config" % ScalikeJdbcVersion,
      "org.postgresql" % "postgresql" % "42.2.18",
    )
  )

lazy val `akka-streams-one` = project
    .in(file("akka-streams-one")) 
    .settings(
      scalaVersion := ScalaVersion,
      libraryDependencies ++= Seq(
        "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
        "com.typesafe.akka" %% "akka-actor-testkit-typed" % AkkaVersion  ,
        "com.typesafe.akka" %% "akka-cluster-typed" % AkkaVersion, //otherwise java.lang.ClassNotFoundException: akka.cluster.ClusterActorRefProvider 
        "org.scalatest" %% "scalatest" % ScalaTest % Test,
      ))

lazy val `persistence-query` = project
    .in(file("persistence-query"))
    .dependsOn(persistence)
    .settings(
        scalaVersion := ScalaVersion, 
        libraryDependencies ++= Seq(
          "com.typesafe.akka" %% "akka-cluster-typed" % AkkaVersion,
          "com.typesafe.akka" %% "akka-persistence-query" % AkkaVersion, 
          "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
          "com.lightbend.akka" %% "akka-persistence-jdbc" % "5.0.0",
          "org.postgresql" % "postgresql" % "42.2.18",
          "com.lightbend.akka" %% "akka-stream-alpakka-slick" % "2.0.0",
          "ch.qos.logback" % "logback-classic" % LogbackVersion,
          "com.typesafe.akka" %% "akka-serialization-jackson" % AkkaVersion,
          "com.typesafe.akka" %% "akka-actor-testkit-typed" % AkkaVersion % Test,
          "org.scalatest" %% "scalatest" % ScalaTest % Test,
          "com.typesafe.akka" %% "akka-stream-testkit" % AkkaVersion % Test
          )

    )

lazy val clustering2 = project
  .in(file("clustering2"))
  .settings(
      scalaVersion := ScalaVersion,
      libraryDependencies ++= Seq(
        "com.typesafe.akka" %% "akka-cluster-typed" % AkkaVersion,
        "com.typesafe.akka" %% "akka-discovery" % AkkaVersion,
        "com.typesafe.akka" %% "akka-cluster" % AkkaVersion,
        "com.lightbend.akka.management" %% "akka-management-cluster-bootstrap" % AkkaManagementVersion,
        "ch.qos.logback" % "logback-classic" % LogbackVersion,
      )
  )


lazy val clustering3 = project
  .in(file("clustering3"))
  .settings(
      scalaVersion := ScalaVersion,
      libraryDependencies ++= Seq(
        "com.typesafe.akka" %% "akka-cluster-typed" % AkkaVersion,
        "com.typesafe.akka" %% "akka-discovery" % AkkaVersion,
        "com.typesafe.akka" %% "akka-cluster" % AkkaVersion,
        "com.lightbend.akka.management" %% "akka-management-cluster-bootstrap" % AkkaManagementVersion,
        "ch.qos.logback" % "logback-classic" % LogbackVersion,
      )
)

lazy val `akka-http` = project
    .in(file("akka-http"))
    .settings(
      scalaVersion := ScalaVersion,
      libraryDependencies ++= Seq(
        "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
        "com.typesafe.akka" %% "akka-cluster-sharding-typed" % AkkaVersion,
        "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
        "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion,
        "com.typesafe.akka" %% "akka-http-spray-json" % AkkaHttpVersion,
        "ch.qos.logback" % "logback-classic" % LogbackVersion
        )
    )
lazy val gRPC = project
    .in(file("gRPC"))
    .enablePlugins(AkkaGrpcPlugin)
    .settings(
      scalaVersion := ScalaVersion,
      libraryDependencies ++= Seq(
        "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
        "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
        "com.typesafe.akka" %% "akka-discovery" % AkkaVersion,
        "com.typesafe.akka" %% "akka-http2-support" % AkkaHttpVersion,
        "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion,
        "ch.qos.logback" % "logback-classic" % LogbackVersion,
      ))


lazy val `gRPC-actor` = project
    .in(file("gRPC-actor"))
    .enablePlugins(AkkaGrpcPlugin)
    .settings(
      scalaVersion := ScalaVersion,
      libraryDependencies ++= Seq(
        "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
        "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
        "com.typesafe.akka" %% "akka-discovery" % AkkaVersion,
        "com.typesafe.akka" %% "akka-http2-support" % AkkaHttpVersion,
        "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion,
        "com.typesafe.akka" %% "akka-cluster-sharding-typed" % AkkaVersion,
        "ch.qos.logback" % "logback-classic" % LogbackVersion,
      ))

lazy val `akka-streams-two` = project
    .in(file("akka-streams-two"))
    .enablePlugins(AkkaGrpcPlugin)//grpc example
    .settings(
      scalafmtOnCompile := true,
      scalaVersion := ScalaVersion,
      libraryDependencies ++= Seq(
       "com.typesafe.akka" %% "akka-stream" % AkkaVersion, 
       "com.typesafe.akka" %% "akka-stream-typed" % AkkaVersion,
       "com.typesafe.akka" %% "akka-actor-testkit-typed" % AkkaVersion,
       "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion,
       "com.typesafe.akka" %% "akka-http-spray-json" % AkkaHttpVersion,
       "com.typesafe.akka" %% "akka-discovery" % AkkaVersion,//grpc example
       "com.typesafe.akka" %% "akka-http2-support" % AkkaHttpVersion,//grpc example
       "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion,//grpc example
       "org.scalikejdbc" %% "scalikejdbc" % "3.5.0",
       "org.postgresql" % "postgresql" % "42.2.18" % Test, 
       "org.scalikejdbc" %% "scalikejdbc-config"  % "3.5.0",
       "org.scalatest" %% "scalatest" % ScalaTest % Test,
       "ch.qos.logback" % "logback-classic" % LogbackVersion, 
      ))

lazy val alpakka = project
    .in(file("alpakka"))
    .settings(
      scalafmtOnCompile := true,
      scalaVersion := ScalaVersion,
      libraryDependencies ++= Seq(
        "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
        "com.typesafe.akka" %% "akka-stream-typed" % AkkaVersion,
        "com.lightbend.akka" %% "akka-stream-alpakka-csv" % "3.0.3",
        "com.lightbend.akka" %% "akka-stream-alpakka-file" % "3.0.3",
        "com.lightbend.akka" %% "akka-stream-alpakka-s3" % "3.0.3",
        "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
        "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion,//
        "com.typesafe.akka" %% "akka-http-xml" % AkkaHttpVersion,// this is for solving dependency version mismatches
        "com.typesafe.akka" %% "akka-stream-kafka" % "2.1.1",
        "com.fasterxml.jackson.core" % "jackson-databind" % JacksonVersion, 
        "ch.qos.logback" % "logback-classic" % LogbackVersion,
        "org.scalatest" %% "scalatest" % ScalaTest % Test, 
        ))

lazy val `betting-house` = project
    .in(file("betting-house"))
    .enablePlugins(AkkaGrpcPlugin, JavaAppPackaging, DockerPlugin)
    .settings(
      version := "0.1.2-SNAPSHOT",
      dockerUsername := Some("franciscolopezsancho"), // assumes docker.io by default
      scalafmtOnCompile := true,
      Compile / mainClass := Some("example.betting.Main"), 
      scalaVersion := ScalaVersion,
      dockerExposedPorts := Seq(8558, 2552, 9000, 9001, 9002, 9003),
      dockerBaseImage := "adoptopenjdk:11-jre-hotspot",
      libraryDependencies ++= Seq(
        "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
        "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
        "com.typesafe.akka" %% "akka-discovery" % AkkaVersion,
        "com.typesafe.akka" %% "akka-http2-support" % AkkaHttpVersion,
        "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion,
        "com.typesafe.akka" %% "akka-cluster-sharding-typed" % AkkaVersion,
        "com.typesafe.akka" %% "akka-cluster-typed" % AkkaVersion,         
        "com.typesafe.akka" %% "akka-cluster" % AkkaVersion, //needed?
        "com.typesafe.akka" %% "akka-serialization-jackson" % AkkaVersion,
        "com.typesafe.akka" %% "akka-persistence-typed" % AkkaVersion,
        "com.typesafe.akka" %% "akka-persistence-testkit" % AkkaVersion % Test,
        "com.lightbend.akka" %% "akka-persistence-jdbc" % "5.0.0",
        "ch.qos.logback" % "logback-classic" % LogbackVersion,
        "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion,
        "com.typesafe.akka" %% "akka-http-spray-json" % AkkaHttpVersion,
        "com.lightbend.akka.management" %% "akka-management-cluster-bootstrap" % AkkaManagementVersion,
        "com.lightbend.akka.management" %% "akka-management-cluster-http" % AkkaManagementVersion,
        "com.lightbend.akka.discovery" %% "akka-discovery-kubernetes-api" % AkkaManagementVersion,
        "org.scalatest" %% "scalatest" % ScalaTest % Test, 
          "org.scalikejdbc" %% "scalikejdbc"       % ScalikeJdbcVersion,
      "org.scalikejdbc" %% "scalikejdbc-config" % ScalikeJdbcVersion,
      "org.postgresql" % "postgresql" % "42.2.18",
       "com.typesafe.akka" %% "akka-stream-kafka" % "2.1.1",
      "com.lightbend.akka" %% "akka-projection-core" % AkkaProjectionVersion,
      "com.lightbend.akka" %% "akka-projection-eventsourced" % AkkaProjectionVersion,
      "com.lightbend.akka" %% "akka-projection-jdbc" % AkkaProjectionVersion
      ))

ThisBuild / watchTriggeredMessage := Watch.clearScreenOnTrigger