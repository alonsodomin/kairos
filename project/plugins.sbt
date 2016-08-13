// Comment to get more information during initialization
logLevel := Level.Warn

addSbtPlugin("io.spray"          % "sbt-revolver"        % "0.8.0")
addSbtPlugin("org.scala-js"      % "sbt-scalajs"         % "0.6.11")
addSbtPlugin("com.typesafe.sbt"  % "sbt-native-packager" % "1.2.0-M5")
addSbtPlugin("com.typesafe.sbt"  % "sbt-multi-jvm"       % "0.3.11")
addSbtPlugin("org.madoushi.sbt"  % "sbt-sass"            % "0.9.3")
addSbtPlugin("de.heikoseeberger" % "sbt-header"          % "1.5.1")
addSbtPlugin("org.scoverage"     % "sbt-scoverage"       % "1.3.5")
addSbtPlugin("com.jsuereth"      % "sbt-pgp"             % "1.0.0")
addSbtPlugin("com.github.gseitz" % "sbt-release"         % "1.0.3")
addSbtPlugin("org.xerial.sbt"    % "sbt-sonatype"        % "1.1")
