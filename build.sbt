
organization in ThisBuild := "io.kairos"

version in ThisBuild := Commons.kairosVersion

scalaVersion in ThisBuild := "2.11.7"

scalacOptions in ThisBuild ++= Seq(
  "-Xexperimental",
  "-language:postfixOps",
  "-feature",
  "-unchecked",
  "-deprecation"
)

resolvers in ThisBuild ++= Seq(
  Opts.resolver.mavenLocalFile,
  "Akka Snapshot Repository" at "http://repo.akka.io/snapshots/",
  "dnvriend at bintray" at "http://dl.bintray.com/dnvriend/maven",
  "OJO Snapshots" at "https://oss.jfrog.org/oss-snapshot-local"
)

coverageHighlighting in ThisBuild := true

lazy val kairos = (project in file(".")).aggregate(
  common, network, resolver, client, cluster, scheduler, examples, worker
)

lazy val examples = (project in file("examples")).aggregate(
  exampleJobs, exampleProducers
)

lazy val common = (project in file("common")).
  settings(Commons.settings: _*).
  settings(
    libraryDependencies ++= Dependencies.commonLibs
  )

lazy val network = (project in file("network")).
  settings(Commons.settings: _*).
  settings(
    libraryDependencies ++= Dependencies.networkLibs
  ).
  dependsOn(common)

lazy val resolver = (project in file("resolver")).
  settings(Commons.settings: _*).
  settings(
    libraryDependencies ++= Dependencies.resolverLibs
  ).
  dependsOn(common).
  dependsOn(network)

lazy val client = (project in file("client")).
  settings(Commons.settings: _*).
  settings(
    libraryDependencies ++= Dependencies.clientLibs
  ).
  dependsOn(common).
  dependsOn(network)

lazy val cluster = (project in file("cluster")).
  settings(Commons.settings: _*).
  settings(
    libraryDependencies ++= Dependencies.clusterLibs
  ).
  dependsOn(common).
  dependsOn(network)

lazy val scheduler = MultiNode(project in file("scheduler")).
  settings(Commons.settings: _*).
  settings(Revolver.settings: _*).
  settings(
    libraryDependencies ++= Dependencies.schedulerLibs,
    parallelExecution in Test := false
  ).
  enablePlugins(JavaServerAppPackaging).
  settings(Packaging.universalServerSettings: _*).
  enablePlugins(DockerPlugin).
  settings(Packaging.schedulerDockerSettings: _*).
  dependsOn(common).
  dependsOn(network).
  dependsOn(resolver).
  dependsOn(cluster)

lazy val worker = (project in file("worker")).
  settings(Commons.settings: _*).
  settings(Revolver.settings: _*).
  settings(
    libraryDependencies ++= Dependencies.workerLibs
  ).
  enablePlugins(JavaServerAppPackaging).
  settings(Packaging.universalServerSettings: _*).
  enablePlugins(DockerPlugin).
  settings(Packaging.workerDockerSettings: _*).
  dependsOn(common).
  dependsOn(network).
  dependsOn(resolver).
  dependsOn(cluster)

lazy val exampleJobs = Project("example-jobs", file("examples/jobs")).
  settings(Commons.settings: _*).
  settings(
    libraryDependencies ++= Dependencies.exampleJobsLibs
  ).
  dependsOn(common)

lazy val exampleProducers = Project("example-producers", file("examples/producers")).
  settings(Commons.settings: _*).
  settings(Revolver.settings: _*).
  settings(
    libraryDependencies ++= Dependencies.exampleProducersLibs
  ).
  enablePlugins(JavaAppPackaging).
  settings(Packaging.universalSettings: _*).
  enablePlugins(DockerPlugin).
  settings(Packaging.dockerSettings: _*).
  dependsOn(common).
  dependsOn(exampleJobs).
  dependsOn(client)
