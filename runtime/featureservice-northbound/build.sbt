name := "InocybeDB"

version := "1.0"

organization := "Inocybe Technologies"

scalaVersion := "2.10.0"

resolvers += Classpaths.typesafeResolver

resolvers += "Typesafe Snapshots" at "http://repo.typesafe.com/typesafe/snapshots/"

resolvers += "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += "Spray.io Nightly" at "http://nightlies.spray.io/"

resolvers += "spray repo" at "http://repo.spray.io"

libraryDependencies += "com.typesafe.akka" % "akka-actor_2.10" % "2.2.0"

libraryDependencies += "com.typesafe.akka" %% "akka-cluster" % "2.2.0"

libraryDependencies += "com.typesafe.akka" %% "akka-contrib" % "2.2.0"

libraryDependencies += "io.spray" %  "spray-can" % "1.2-20130712"

libraryDependencies += "io.spray" %  "spray-routing" % "1.1-M7"

libraryDependencies += "io.spray" %  "spray-json_2.10" % "1.2.3"
