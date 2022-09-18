val AkkaVersion = "2.6.14"
val LogbackVersion = "1.2.3"
val AkkaManagementVersion = "1.1.0"

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.13.1"
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-cluster-typed" % AkkaVersion,
  "com.typesafe.akka" %% "akka-discovery" % AkkaVersion,
  "com.typesafe.akka" %% "akka-cluster" % AkkaVersion,
  "com.lightbend.akka.management" %% "akka-management-cluster-bootstrap" % AkkaManagementVersion,
  "com.lightbend.akka.management" %% "akka-management-cluster-http" % AkkaManagementVersion,
  "com.lightbend.akka.discovery" %% "akka-discovery-kubernetes-api" % AkkaManagementVersion,
  "ch.qos.logback" % "logback-classic" % LogbackVersion,

)


enablePlugins(JavaAppPackaging, DockerPlugin)
dockerExposedPorts := Seq(8558, 2552)
dockerBaseImage := "adoptopenjdk:11-jre-hotspot"
