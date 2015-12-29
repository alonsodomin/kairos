import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import sbt._

object Dependencies {

  object version {

    // Logging -------

    val slf4s = "1.7.12"
    val log4j = "2.3"

    // Testing --------

    val scalaTest = "3.0.0-M10"
    val scalaMock = "3.2.2"

    // Akka ----------

    val akka = "2.4.1"
    val akkaHttp = "2.0-M2"
    val akkaStreaming = "2.0-M2"

    // ScalaJS -------

    val scalaJsReact = "0.10.2"
    val scalaCss = "0.3.1"

    val reactJs = "0.14.3"

    // Other utils ---

    val scopt = "3.3.0"
    val monocle = "1.1.1"
    val scalaz = "7.1.3"
  }

  // Common library definitions

  object libs {

    val scalaXml = "org.scala-lang.modules" %% "scala-xml" % "1.0.5"

    val ivy = "org.apache.ivy" % "ivy" % "2.4.0"

    object Akka {
      val actor          = "com.typesafe.akka" %% "akka-actor"             % version.akka
      val clusterTools   = "com.typesafe.akka" %% "akka-cluster-tools"     % version.akka
      val clusterMetrics = "com.typesafe.akka" %% "akka-cluster-metrics"   % version.akka
      val sharding       = "com.typesafe.akka" %% "akka-cluster-sharding"  % version.akka
      val http           = "com.typesafe.akka" %% "akka-http-experimental" % version.akkaHttp
      val httpUpickle    = "de.heikoseeberger" %% "akka-http-upickle"      % "1.3.0"
      val sse            = "de.heikoseeberger" %% "akka-sse"               % "1.3.0"

      object persistence {
        val core      = "com.typesafe.akka"   %% "akka-persistence"              % version.akka
        val query     = "com.typesafe.akka"   %% "akka-persistence-query-experimental" % version.akka
        val jdbc      = "com.github.dnvriend" %% "akka-persistence-jdbc"         % "1.2.2"
        val cassandra = "com.github.krasserm" %% "akka-persistence-cassandra-3x" % "0.6"
        val memory    = "com.github.dnvriend" %% "akka-persistence-inmemory"     % "1.1.5"
      }

      val multiNodeTestKit = "com.typesafe.akka" %% "akka-multi-node-testkit" % version.akka
      val testKit          = "com.typesafe.akka" %% "akka-testkit"            % version.akka % Test
    }
    val akka = Seq(Akka.actor, Akka.clusterTools, Akka.clusterMetrics, Akka.testKit)

    object Log4j {
      val api       = "org.apache.logging.log4j"  % "log4j-api"        % version.log4j
      val core      = "org.apache.logging.log4j"  % "log4j-core"       % version.log4j
      val slf4jImpl = "org.apache.logging.log4j"  % "log4j-slf4j-impl" % version.log4j % Runtime
    }
    val slf4s = "org.slf4s" %% "slf4s-api" % version.slf4s
    val logging = Seq(slf4s, Log4j.api, Log4j.core, Log4j.slf4jImpl)

    val scopt = "com.github.scopt" %% "scopt" % version.scopt

    val scalaTest = "org.scalatest" %% "scalatest"                   % version.scalaTest
    val scalaMock = "org.scalamock" %% "scalamock-scalatest-support" % version.scalaMock

    val testing = Seq(scalaTest, scalaMock).map(_ % Test)

    val scalaz = "org.scalaz" %% "scalaz-core" % version.scalaz

    object ScalaJsReact {
      val core  = Def.setting { "com.github.japgolly.scalajs-react" %%% "core"  % version.scalaJsReact }
      val extra = Def.setting { "com.github.japgolly.scalajs-react" %%% "extra" % version.scalaJsReact }
      val test  = Def.setting { "com.github.japgolly.scalajs-react" %%% "test"  % version.scalaJsReact % Test }
    }

    object ScalaCss {
      val core  = Def.setting { "com.github.japgolly.scalacss" %%% "core"      % version.scalaCss }
      val react = Def.setting { "com.github.japgolly.scalacss" %%% "ext-react" % version.scalaCss }
    }

  }

  object module {
    import libs._

    val common   = testing ++ Seq(scalaXml)
    val network  = logging ++ testing ++ akka
    val client   = logging ++ testing ++ akka
    val cluster  = logging ++ testing ++ akka ++ Seq(
      ivy, scalaXml, scalaz
    )

    val kernel = logging ++ testing ++ akka ++ Seq(
      Akka.persistence.core,
      Akka.persistence.cassandra,
      Akka.persistence.query,
      Akka.persistence.memory % Test,
      Akka.sharding, Akka.http, Akka.httpUpickle, Akka.sse,
      scopt, scalaz
    )

    val worker = logging ++ testing ++ akka :+ scopt

    val exampleJobs = logging
    val exampleProducers = logging ++ testing ++ akka :+ scopt

  }

}
