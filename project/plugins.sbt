// Comment to get more information during initialization
logLevel := Level.Warn

resolvers += Resolver.url("scoverage-bintray", url("https://dl.bintray.com/sksamuel/sbt-plugins/"))(Resolver.ivyStylePatterns)

addSbtPlugin("io.spray" % "sbt-revolver" % "0.8.0")
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.7")
addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.0.4")

// IntelliJ plugin
addSbtPlugin("com.github.mpeltonen" % "sbt-idea" % "1.6.0")

addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.3.3")
addSbtPlugin("org.scoverage" % "sbt-coveralls" % "1.0.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-multi-jvm" % "0.3.11")

addSbtPlugin("org.madoushi.sbt" % "sbt-sass" % "0.9.3")
