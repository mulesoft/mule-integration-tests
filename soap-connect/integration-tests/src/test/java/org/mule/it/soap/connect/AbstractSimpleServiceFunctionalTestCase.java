/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.it.soap.connect;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.service.soap.TestHttpSoapServer;
import org.mule.service.soap.service.Soap11Service;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.runner.ArtifactClassLoaderRunnerConfig;

import org.junit.Rule;

@ArtifactClassLoaderRunnerConfig(sharedRuntimeLibs = {"org.mule.tests:mule-tests-unit"})
public abstract class AbstractSimpleServiceFunctionalTestCase extends MuleArtifactFunctionalTestCase {

  @Rule
  public DynamicPort port = new DynamicPort("testPort");

  private TestHttpSoapServer server = new TestHttpSoapServer(port.getNumber(), new Soap11Service());

  @Rule
  public SystemProperty systemProperty = new SystemProperty("address", server.getDefaultAddress());

  @Override
  protected void doSetUp() throws Exception {
    server.init();
  }

  @Override
  protected void doTearDown() throws Exception {
    server.stop();
  }
}
