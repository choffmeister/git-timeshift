name := "git-timeshift"

version := "0.0.1-SNAPSHOT"

organization := "de.choffmeister"

scalaVersion := "2.11.0"

scalacOptions := Seq("-encoding", "utf8")

libraryDependencies ++= Seq(
  "com.madgag" %% "bfg-library" % "1.11.10",
  "org.specs2" %% "specs2" % "2.3.11" % "test"
)
