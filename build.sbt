name := "monix-streaming-app"

version := "0.1"

scalaVersion := "2.12.10"

enablePlugins(DockerComposePlugin)

libraryDependencies += "io.monix" %% "monix" % "3.1.0"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.1.1" % "test"
