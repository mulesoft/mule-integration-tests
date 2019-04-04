/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.it.soap.connect;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.ExternalProcess;
import org.mule.tck.junit4.rule.SystemProperty;

import org.junit.Rule;

public abstract class SoapFootballExtensionArtifactFunctionalTestCase extends MuleArtifactFunctionalTestCase {

  private static final String SOAP_CONFIG = "soap-football-extension-config.xml";

  @Rule
  public DynamicPort footballPort = new DynamicPort("footballPort");

  @Rule
  public DynamicPort laLigaPort = new DynamicPort("laLigaPort");

  @Rule
  public final ExternalProcess footballServiceServer =
      new ExternalProcess("java", "-cp", System.getProperty("soapHttpServerClasspath"), "org.mule.service.soap.server.HttpServer",
                          "" + footballPort.getNumber(), "org.mule.it.soap.connect.services.FootballService");
  @Rule
  public final ExternalProcess laLigaServiceServer =
      new ExternalProcess("java", "-cp", System.getProperty("soapHttpServerClasspath"), "org.mule.service.soap.server.HttpServer",
                          "" + laLigaPort.getNumber(), "org.mule.it.soap.connect.services.LaLigaService");

  @Rule
  public SystemProperty footballAddress =
      new SystemProperty("footballAddress", "http://localhost:" + footballPort.getNumber() + "/server");

  @Rule
  public SystemProperty laLigaAddress =
      new SystemProperty("laLigaAddress", "http://localhost:" + laLigaPort.getNumber() + "/server");

  @Override
  protected String getConfigFile() {
    return SOAP_CONFIG;
  }

  String getBodyXml(String tagName, String content) {
    String ns = "http://services.connect.soap.it.mule.org/";
    return String.format("<con:%s xmlns:con=\"%s\">%s</con:%s>", tagName, ns, content, tagName);
  }

}
