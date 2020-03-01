name := "functional-streaming-app"

version := "0.1"

scalaVersion := "2.12.10"

enablePlugins(DockerComposePlugin)

libraryDependencies += "io.monix" %% "monix" % "3.1.0"
libraryDependencies += "org.apache.kafka" % "kafka-clients" % "2.4.0"
libraryDependencies += "org.apache.pulsar" % "pulsar-client" % "2.5.0"
libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2"
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.1.1" % "test"
