name := """splunk-logback-hec-appender"""

version := "1.0"

scalaVersion := "2.11.8"

scalacOptions += "-target:jvm-1.8"

unmanagedResourceDirectories in Compile += baseDirectory.value / "conf"

libraryDependencies ++= Seq(

  "org.skinny-framework" %% "skinny-http-client" % "2.2.0",

  "org.json4s" %% "json4s-native" % "3.4.0",

  "io.monix" %% "monix" % "2.0.1",

  "ch.qos.logback" % "logback-core" % "1.1.7",
  "ch.qos.logback" % "logback-classic" % "1.1.7",
//  "com.splunk.logging" % "splunk-library-javalogging" % "1.5.1",

  "org.scalatest" %% "scalatest" % "2.2.4" % "test"

)


