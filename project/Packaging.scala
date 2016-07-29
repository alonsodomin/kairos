import com.typesafe.sbt.packager.archetypes.JavaAppPackaging.autoImport._
import com.typesafe.sbt.packager.docker.Cmd
import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport._
import com.typesafe.sbt.packager.linux.LinuxPlugin.autoImport._

object Packaging {

  private[this] val linuxHomeLocation = "/opt/quckoo"

  lazy val universalSettings = Seq(
    bashScriptExtraDefines ++= Seq(
      """addJava "-Dlog4j.configurationFile=${app_home}/../conf/log4j2.xml""""
    )
  )

  lazy val universalServerSettings = Seq(
    bashScriptExtraDefines ++= Seq(
      """addJava "-Dconfig.file=${app_home}/../conf/application.conf"""",
      """addJava "-Dlog4j.configurationFile=${app_home}/../conf/log4j2.xml""""
    )
  )

  lazy val baseDockerSettings = Seq(
    dockerRepository := Some("quckoo"),
    dockerUpdateLatest := true,
    dockerExposedVolumes := Seq(
      s"$linuxHomeLocation/conf"
    ),
    defaultLinuxInstallLocation in Docker := linuxHomeLocation
  )

  lazy val serverDockerSettings = baseDockerSettings ++ Seq(
    dockerExposedVolumes ++= Seq(
      s"$linuxHomeLocation/resolver/cache",
      s"$linuxHomeLocation/resolver/local"
    ),
    dockerCommands ++= Seq(
      Cmd("ENV", "QUCKOO_HOME", linuxHomeLocation)
    )
  )

  lazy val masterDockerSettings = serverDockerSettings ++ Seq(
    dockerExposedPorts := Seq(2551, 8095)
  )

  lazy val workerDockerSettings = serverDockerSettings ++ Seq(
    dockerExposedPorts := Seq(5001)
  )

}