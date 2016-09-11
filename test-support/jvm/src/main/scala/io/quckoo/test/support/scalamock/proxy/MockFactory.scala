package io.quckoo.test.support.scalamock.proxy

import io.quckoo.test.support.scalamock.AbstractMockFactory
import org.scalamock.proxy.ProxyMockFactory
import org.scalatest.TestSuite

/**
  * Created by alonsodomin on 04/09/2016.
  */
trait MockFactory extends AbstractMockFactory with ProxyMockFactory with TestSuite