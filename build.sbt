name := """transaction-validator"""
organization := "com.swan"
version := "1.0-SNAPSHOT"

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)

scalaVersion := "2.13.15"

libraryDependencies += guice
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.1" % Test
libraryDependencies += "org.webjars" % "swagger-ui" % "5.17.14"

scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-unchecked",
  "-Xfatal-warnings"
)

import sbtassembly.MergeStrategy

assembly / assemblyJarName := "transaction-validator.jar"
assembly / mainClass := Some("play.core.server.ProdServerStart")
assembly / fullClasspath += Attributed.blank(PlayKeys.playPackageAssets.value)

assembly / assemblyMergeStrategy := {
  case PathList(ps @ _*) if ps.last == "reference-overrides.conf" => MergeStrategy.concat
  case PathList(ps @ _*) if ps.last == "reference.conf"           => MergeStrategy.concat
  case "module-info.class"                                        => MergeStrategy.discard
  case PathList("META-INF", "services", xs @ _*)                   => MergeStrategy.concat
  case PathList("META-INF", "io.netty.versions.properties")       => MergeStrategy.first
  case PathList("META-INF", xs @ _*)                               => MergeStrategy.discard
  case x =>
    val oldStrategy = (assembly / assemblyMergeStrategy).value
    oldStrategy(x)
}
