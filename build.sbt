name := "lift-messagebus"

organization := "net.liftmodules"

version := "1.0"

liftVersion <<= liftVersion ?? "3.0-RC1"

liftEdition <<= liftVersion apply { _.substring(0,3) }

moduleName <<= (name, liftEdition) { (n, e) =>  n + "_" + e }

scalaVersion := "2.11.7"

crossScalaVersions := Seq("2.11.0", "2.10.0", "2.9.2", "2.9.1-1", "2.9.1")

scalacOptions ++= Seq("-unchecked", "-deprecation")

scalacOptions in Test ++= Seq("-Yrangepos")

resolvers ++= Seq("snapshots"      at "https://oss.sonatype.org/content/repositories/snapshots",
                  "releases"       at "https://oss.sonatype.org/content/repositories/releases",
                  "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"
                 )

libraryDependencies <++= liftVersion { v =>
  Seq(
    "net.liftweb" %% "lift-webkit" % v         % "provided",
    "org.specs2"  %% "specs2-core" % "3.0"     % "test"
  )
}

//publishTo in ThisBuild := Some(Resolver.file("file",  new File(Path.userHome.absolutePath+"/.m2/repository")))
publishTo <<= version { _.endsWith("SNAPSHOT") match {
    case true  => Some("snapshots" at "https://oss.sonatype.org/content/repositories/snapshots")
    case false => Some("releases" at "https://oss.sonatype.org/service/local/staging/deploy/maven2")
  }
}

// For local deployment:
credentials += Credentials( file("sonatype.credentials") )

// For the build server:
credentials += Credentials( file("/private/liftmodules/sonatype.credentials") )

publishMavenStyle := true

publishArtifact in Test := false

pomIncludeRepository := { _ => false }


pomExtra := (
  <url>https://github.com/pdyraga/lift-message-bus</url>
  <licenses>
    <license>
      <name>Apache 2.0 License</name>
      <url>http://www.apache.org/licenses/LICENSE-2.0.html</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
  <scm>
    <url>git@github.com:pdyraga/lift-message-bus.git</url>
    <connection>scm:git:git@github.com:pdyraga/lift-message-bus.git</connection>
  </scm>
  <developers>
    <developer>
      <id>piotrd</id>
      <name>Piotr Dyraga</name>
      <url>http://www.ontheserverside.com</url>
    </developer>
  </developers>
)