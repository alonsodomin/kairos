import sbtcrossproject.{crossProject, CrossType}
import com.typesafe.sbt.pgp.PgpKeys
import scala.xml.transform.{RewriteRule, RuleTransformer}

lazy val sandbox =
  settingKey[String]("The name of the environment sandbox to use.")

inThisBuild(
  Seq(
    parallelExecution := false,
    scalafmtVersion := Dependencies.version.scalafmt,
    scalafmtOnCompile := true
  )
)

lazy val commonSettings = Seq(
  homepage := Some(url("https://www.quckoo.io")),
  licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.txt")),
  organization := "io.quckoo",
  organizationName := "A. Alonso Dominguez",
  startYear := Some(2015),
  scmInfo := Some(
    ScmInfo(
      url("https://www.github.com/alonsodomin/quckoo"),
      "scm:git:git@github.com:alonsodomin/quckoo.git"
    )
  ),
  scalacOptions ++= Seq(
    "-encoding",
    "UTF-8",
    "-language:postfixOps",
    "-language:higherKinds",
    "-feature",
    "-unchecked",
    "-deprecation",
    "-Xlint:-unused,_",
    "-Xfuture",
    "-Xfatal-warnings",
    "-Ywarn-dead-code",
    "-Ywarn-numeric-widen",
    "-Ypartial-unification"
  ),
  scalaModuleInfo := scalaModuleInfo.value.map(_.withOverrideScalaVersion(true)),
  resolvers ++= Seq(
    Opts.resolver.mavenLocalFile,
    Resolver.bintrayRepo("krasserm", "maven"),
    Resolver.bintrayRepo("hseeberger", "maven"),
    Resolver.bintrayRepo("dnvriend", "maven"),
    Resolver.bintrayRepo("tecsisa", "maven-bintray-repo")
  )
)

lazy val commonJvmSettings = Seq(
  fork in Test := false
)

lazy val commonJsSettings = Seq(
  coverageEnabled := false,
  scalaJSStage in Test := FastOptStage,
  jsEnv in Test := PhantomJSEnv().value,
  // batch mode decreases the amount of memory needed to compile scala.js code
  scalaJSOptimizerOptions := scalaJSOptimizerOptions.value.withBatchMode(isTravisBuild.value)
)

lazy val scoverageSettings = Seq(
  coverageHighlighting := true,
  coverageExcludedPackages := "io\\.quckoo\\.console\\.html\\..*"
)

lazy val publishSettings = Seq(
  publishMavenStyle := true,
  publishArtifact in Test := false,
  publishTo := Some(
    if (isSnapshot.value) Opts.resolver.sonatypeSnapshots
    else Opts.resolver.sonatypeStaging
  ),
  // don't include scoverage as a dependency in the pom
  // see issue #980
  // this code was copied from https://github.com/mongodb/mongo-spark
  pomPostProcess := { (node: xml.Node) =>
    new RuleTransformer(new RewriteRule {
      override def transform(node: xml.Node): Seq[xml.Node] = node match {
        case e: xml.Elem
            if e.label == "dependency" && e.child
              .exists(child => child.label == "groupId" && child.text == "org.scoverage") =>
          Nil
        case _ => Seq(node)
      }
    }).transform(node).head
  },
  pomExtra :=
    <developers>
      <developer>
        <id>alonsodomin</id>
        <name>Antonio Alonso Dominguez</name>
        <url>https://github.com/alonsodomin</url>
      </developer>
    </developers>
)

lazy val noPublishSettings = publishSettings ++ Seq(
  skip in publish := true,
  publishArtifact := false
)

lazy val releaseSettings = {
  import ReleaseTransformations._

  val dockerRelease = ReleaseStep(action = st => {
    val extracted              = Project.extract(st)
    val projectRef: ProjectRef = extracted.get(thisProjectRef)
    extracted.runAggregated(publish in Docker in projectRef, st)
    st
  })

  Seq(
    releasePublishArtifactsAction := PgpKeys.publishSigned.value,
    releaseProcess := Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      runClean,
      runTest,
      setReleaseVersion,
      commitReleaseVersion,
      tagRelease,
      publishArtifacts,
      releaseStepCommand("sonatypeReleaseAll"),
      dockerRelease,
      setNextVersion,
      commitNextVersion,
      pushChanges
    )
  )
}

lazy val quckoo = (project in file("."))
  .enablePlugins(AutomateHeaderPlugin, DockerComposePlugin)
  .settings(commonSettings)
  .settings(
    name := "quckoo",
    moduleName := "quckoo-root",
    sandbox := "standalone",
    dockerImageCreationTask := (publishLocal in Docker).value,
    composeFile := s"./sandbox/${sandbox.value}/docker-compose.yml"
  )
  .settings(noPublishSettings)
  .settings(releaseSettings)
  .aggregate(
    coreJS,
    coreJVM,
    apiJS,
    apiJVM,
    clientJS,
    clientJVM,
    console,
    shared,
    master,
    worker,
    examples,
    utilJS,
    utilJVM,
    testSupportJS,
    testSupportJVM
  )

// Core ==================================================

lazy val core =
  (crossProject(JSPlatform, JVMPlatform).crossType(CrossType.Pure) in file("core"))
    .enablePlugins(BuildInfoPlugin, AutomateHeaderPlugin)
    .settings(
      name := "core",
      moduleName := "quckoo-core",
      buildInfoPackage := "io.quckoo",
      buildInfoKeys := Seq[BuildInfoKey](version, scalaVersion),
      buildInfoObject := "Info"
    )
    .settings(commonSettings)
    .settings(scoverageSettings)
    .settings(publishSettings)
    .settings(Dependencies.core)
    .jsSettings(commonJsSettings)
    .jsSettings(Dependencies.coreJS)
    .jvmSettings(commonJvmSettings)
    .jvmSettings(Dependencies.coreJVM)
    .dependsOn(util, testSupport % Test)

lazy val coreJS  = core.js
lazy val coreJVM = core.jvm

// API ==================================================

lazy val api =
  (crossProject(JSPlatform, JVMPlatform).crossType(CrossType.Pure) in file("api"))
    .enablePlugins(AutomateHeaderPlugin)
    .settings(commonSettings)
    .settings(scoverageSettings)
    .settings(publishSettings)
    .settings(Dependencies.api)
    .jsSettings(commonJsSettings)
    .jvmSettings(commonJvmSettings)
    .settings(
      name := "api",
      moduleName := "quckoo-api"
    )
    .dependsOn(core, testSupport % Test)

lazy val apiJS  = api.js
lazy val apiJVM = api.jvm

// Client ==================================================

lazy val client = (crossProject(JSPlatform, JVMPlatform) in file("client"))
  .enablePlugins(AutomateHeaderPlugin)
  .settings(commonSettings)
  .settings(scoverageSettings)
  .settings(publishSettings)
  .settings(Dependencies.client)
  .jsSettings(commonJsSettings)
  .jsSettings(Dependencies.clientJS)
  .jvmSettings(commonJvmSettings)
  .jvmSettings(Dependencies.clientJVM)
  .settings(
    name := "client",
    moduleName := "quckoo-client"
  )
  .dependsOn(api, testSupport % Test)

lazy val clientJS  = client.js
lazy val clientJVM = client.jvm

// Console ==================================================

lazy val console = (project in file("console"))
  .enablePlugins(AutomateHeaderPlugin, ScalaJSPlugin, ScalaJSWeb)
  .settings(commonSettings)
  .settings(commonJsSettings)
  .settings(publishSettings)
  .settings(Dependencies.console)
  .settings(
    name := "console",
    moduleName := "quckoo-console",
    scalaJSUseMainModuleInitializer in Compile := true
  )
  .dependsOn(clientJS, testSupportJS % Test)

// Server ==================================================

lazy val shared = (project in file("shared"))
  .enablePlugins(AutomateHeaderPlugin)
  .settings(commonSettings)
  .settings(commonJvmSettings)
  .settings(scoverageSettings)
  .settings(publishSettings)
  .settings(Dependencies.clusterShared)
  .settings(
    moduleName := "quckoo-shared"
  )
  .dependsOn(apiJVM, testSupportJVM % Test)

lazy val master = (project in file("master"))
  .enablePlugins(
    AutomateHeaderPlugin,
    QuckooWebServer,
    QuckooServerPackager,
    QuckooMultiJvmTesting
  )
  .settings(commonSettings)
  .settings(commonJvmSettings)
  .settings(scoverageSettings)
  .settings(publishSettings)
  .settings(automateHeaderSettings(MultiJvm))
  .settings(Dependencies.clusterMaster)
  .settings(
    moduleName := "quckoo-master",
    scalaJSProjects := Seq(console),
    dockerExposedPorts := Seq(2551, 8095, 9095),
    parallelExecution in Test := false,
    parallelExecution in MultiJvm := false
  )
  .dependsOn(shared % "compile->compile;test->test", testSupportJVM % Test)

lazy val worker = (project in file("worker"))
  .enablePlugins(AutomateHeaderPlugin, QuckooApp, QuckooServerPackager)
  .settings(commonSettings)
  .settings(commonJvmSettings)
  .settings(scoverageSettings)
  .settings(publishSettings)
  .settings(Dependencies.clusterWorker)
  .settings(
    moduleName := "quckoo-worker",
    dockerExposedPorts := Seq(5001, 9010, 9095),
    parallelExecution in Test := false
  )
  .dependsOn(shared % "compile->compile;test->test", testSupportJVM % Test)

// Misc Utilities ===========================================

lazy val util = (crossProject(JSPlatform, JVMPlatform) in file("util"))
  .enablePlugins(AutomateHeaderPlugin)
  .settings(commonSettings)
  .jsSettings(commonJsSettings)
  .jsSettings(Dependencies.utilJS)
  .jvmSettings(commonJvmSettings)
  .settings(moduleName := "quckoo-util")
  .dependsOn(testSupport % Test)

lazy val utilJS  = util.js
lazy val utilJVM = util.jvm

// Test Support Utilities ===================================

lazy val testSupport =
  (crossProject(JSPlatform, JVMPlatform) in file("test-support"))
    .enablePlugins(AutomateHeaderPlugin)
    .settings(commonSettings)
    .settings(noPublishSettings)
    .settings(Dependencies.testSupport)
    .jsSettings(commonJsSettings)
    .jvmSettings(commonJvmSettings)
    .jvmSettings(Dependencies.testSupportJVM)
    .settings(
      name := "test-support",
      moduleName := "quckoo-test-support"
    )

lazy val testSupportJS  = testSupport.js
lazy val testSupportJVM = testSupport.jvm

// Examples ==================================================

lazy val examples = (project in file("examples"))
  .settings(moduleName := "quckoo-examples")
  .aggregate(exampleJobs, exampleProducers)
  .settings(noPublishSettings)

lazy val exampleJobs = (project in file("examples/jobs"))
  .enablePlugins(AutomateHeaderPlugin)
  .settings(commonSettings)
  .settings(commonJvmSettings)
  .settings(publishSettings)
  .settings(Dependencies.exampleJobs)
  .settings(
    name := "example-jobs",
    moduleName := "quckoo-example-jobs"
  )
  .dependsOn(coreJVM)

lazy val exampleProducers = (project in file("examples/producers"))
  .enablePlugins(AutomateHeaderPlugin, QuckooAppPackager)
  .settings(commonSettings)
  .settings(commonJvmSettings)
  .settings(publishSettings)
  .settings(Revolver.settings)
  .settings(Dependencies.exampleProducers)
  .settings(
    name := "example-producers",
    moduleName := "quckoo-example-producers"
  )
  .dependsOn(clientJVM, exampleJobs)

// Command aliases ==================================================

addCommandAlias(
  "testJS",
  Seq(
    "coreJS/test",
    "apiJS/test",
    "clientJS/test",
    "console/test"
  ).mkString(";", ";", "")
)

addCommandAlias(
  "validate",
  Seq(
    "test",
    "master/multi-jvm:test"
  ).mkString(";", ";", "")
)

addCommandAlias(
  "recompile",
  Seq(
    "clean",
    "test:compile",
    "master/multi-jvm:compile"
  ).mkString(";", ";", "")
)

addCommandAlias(
  "rebuild",
  Seq(
    "clean",
    "validate"
  ).mkString(";", ";", "")
)

addCommandAlias(
  "launchLocal",
  Seq(
    "docker:publishLocal",
    "dockerComposeUp"
  ).mkString(";", ";", "")
)
