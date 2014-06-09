name := "akka-persistence"

version := "1.0"

scalaVersion := "2.10.3"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-persistence-experimental" % "2.3.2",
"com.typesafe.akka" %% "akka-actor" % "2.3.2",
"com.typesafe.akka" %% "akka-remote" % "2.3.2"
)

libraryDependencies += "org.scalatest" % "scalatest_2.16"  %  "2.1.6" % "test"


    