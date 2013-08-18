import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "kwk"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    // Add your project dependencies here,
    javaJdbc,
    javaCore,
    anorm,
    "mysql" % "mysql-connector-java" % "5.1.21",
    "securesocial" %% "securesocial" % "master-SNAPSHOT"
  )


  val main = play.Project(appName, appVersion, appDependencies).settings(
    // Add your own project settings here
    resolvers += Resolver.url("sbt-plugin-snapshots", new URL("http://repo.scala-sbt.org/scalasbt/sbt-plugin-snapshots/"))(Resolver.ivyStylePatterns)
  )

}
