name := "lift-message-bus"
version := "0.0.1"
organization := "net.liftmodules"

scalaVersion := "2.11.7"

resolvers ++= Seq("snapshots"      at "https://oss.sonatype.org/content/repositories/snapshots",
                  "releases"       at "https://oss.sonatype.org/content/repositories/releases",
                  "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"
                 )

scalacOptions in Test ++= Seq("-Yrangepos")

libraryDependencies ++= {
  Seq(
    "net.liftweb" %% "lift-webkit" % "3.0-RC1" % "provided",
    "org.specs2"  %% "specs2-core" % "3.0"     % "test"
  )
}
