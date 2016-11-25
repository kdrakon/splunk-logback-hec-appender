name := """splunk-logback-hec-appender"""

version := "1.0.2"

scalaVersion := "2.11.8"

scalacOptions += "-target:jvm-1.8"

libraryDependencies ++= Seq(

  "org.skinny-framework" %% "skinny-http-client" % "2.3.0",

  "org.json4s" %% "json4s-native" % "3.5.0",

  "io.monix" %% "monix" % "2.1.1",

  "ch.qos.logback" % "logback-core" % "1.1.7",
  "ch.qos.logback" % "logback-classic" % "1.1.7",
//  "com.splunk.logging" % "splunk-library-javalogging" % "1.5.1",

  "org.scalatest" %% "scalatest" % "2.2.6" % "test"

)


