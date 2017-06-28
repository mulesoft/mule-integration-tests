/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.it.soap.connect;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.it.soap.connect.services.FootballService;
import org.mule.it.soap.connect.services.LaLigaService;
import org.mule.service.soap.server.HttpServer;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.runner.ArtifactClassLoaderRunnerConfig;
import org.junit.Rule;

@ArtifactClassLoaderRunnerConfig(sharedRuntimeLibs = {"org.mule.tests:mule-tests-unit"})
public abstract class SoapFootballExtensionArtifactFunctionalTestCase extends MuleArtifactFunctionalTestCase {

  private static final String SOAP_CONFIG = "soap-football-extension-config.xml";

  @Rule
  public DynamicPort footballPort = new DynamicPort("footballPort");

  @Rule
  public DynamicPort laLigaPort = new DynamicPort("laLigaPort");

  private HttpServer footballService = new HttpServer(footballPort.getNumber(), null, null, new FootballService());
  private HttpServer laLigaService = new HttpServer(laLigaPort.getNumber(), null, null, new LaLigaService());

  @Rule
  public SystemProperty footballAddress = new SystemProperty("footballAddress", footballService.getDefaultAddress());

  @Rule
  public SystemProperty laLigaAddress = new SystemProperty("laLigaAddress", laLigaService.getDefaultAddress());

  @Override
  protected String getConfigFile() {
    return SOAP_CONFIG;
  }

  String getBodyXml(String tagName, String content) {
    String ns = "http://services.connect.soap.it.mule.org/";
    return String.format("<con:%s xmlns:con=\"%s\">%s</con:%s>", tagName, ns, content, tagName);
  }

  @Override
  protected void doTearDown() throws Exception {
    super.doTearDown();
    footballService.stop();
    laLigaService.stop();
  }

}
