package io.chronos

import org.apache.ivy.core.settings.IvySettings
import org.apache.ivy.plugins.resolver.ChainResolver
import org.slf4s.Logging

import scala.language.implicitConversions

/**
 * Created by aalonsodominguez on 19/07/2015.
 */
package object resolver extends Logging {

  private[resolver] implicit def convertConfig2Settings(config: IvyConfiguration): IvySettings = {
    implicit val ivySettings = new IvySettings()
    ivySettings.loadDefault()
    ivySettings.setBaseDir(config.baseDir.toFile)
    ivySettings.setDefaultCache(config.cacheDir.toFile)
    config.ivyHome match {
      case Some(home) => ivySettings.setDefaultIvyUserDir(home.toFile)
      case None       =>
    }

    val defaultResolverChain = buildResolverChain("default", IvyConfiguration.DefaultRepositories)
    val userResolverChain = buildResolverChain("user", config.repositories)

    ivySettings.addResolver(defaultResolverChain)
    ivySettings.addResolver(userResolverChain)
    ivySettings.setDefaultResolver(defaultResolverChain.getName)

    log.info("Apache Ivy initialized")
    ivySettings
  }

  private def buildResolverChain(name: String, repos: Seq[Repository])(implicit settings: IvySettings): ChainResolver = {
    val resolver = new ChainResolver
    resolver.setName(name)
    repos.foreach { repo => resolver.add(RepositoryConversion(repo, settings)) }
    resolver
  }

}