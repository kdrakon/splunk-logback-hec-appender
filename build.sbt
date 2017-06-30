name := """splunk-logback-hec-appender"""
version := "1.1.0"
scalaVersion := "2.12.2"
crossScalaVersions := Seq("2.11.11", "2.12.2")
organization := "io.policarp"
homepage := Some(url("https://github.com/kdrakon/splunk-logback-hec-appender"))
scmInfo := Some(
  ScmInfo(
    url("https://github.com/kdrakon/splunk-logback-hec-appender"),
    "scm:git@github.com:kdrakon/splunk-logback-hec-appender.git"
  )
)

scalacOptions += "-target:jvm-1.8"

libraryDependencies ++= Seq(

  "org.skinny-framework" %% "skinny-http-client" % "2.3.0",

  "org.json4s" %% "json4s-native" % "3.5.0",

  "io.monix" %% "monix" % "2.3.0",

  "ch.qos.logback" % "logback-core" % "1.1.7",
  "ch.qos.logback" % "logback-classic" % "1.1.7",
//  "com.splunk.logging" % "splunk-library-javalogging" % "1.5.1",

  "org.scalatest" %% "scalatest" % "3.0.1" % "test"

)

pomIncludeRepository := { _ => false }
publishMavenStyle := true
licenses := Seq("Apache License 2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0.txt"))
developers := List(
  Developer(
    id    = "kdrakon",
    name  = "Sean Policarpio",
    email = "",
    url   = url("http://policarp.io")
  )
)
useGpg := true
publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}


