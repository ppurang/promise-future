name := "promise-future"

version := "0.0.1"

organization := "org.purang.concurrency"

scalaVersion := "2.10.0-M5"

libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-actors" % "2.10.0-M5" withSources(),
  "org.scalatest" % "scalatest_2.10.0-M5" % "1.9-2.10.0-M5-B2" % "test" withSources()
  )

scalacOptions ++= Seq("-encoding", "UTF-8", "-deprecation", "-unchecked", "-feature", "-language:*")

cancelable := true

offline := true

resolvers += "typesafe snapshot" at "http://repo.typesafe.com/typesafe/snapshots"