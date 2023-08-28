/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.it.soap.connect;

import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.ExternalProcess;
import org.mule.tck.junit4.rule.SystemProperty;

import org.junit.Rule;

public abstract class SoapFootballExtensionArtifactFunctionalTestCase extends MuleArtifactFunctionalTestCase {

  private static final String SOAP_CONFIG = "soap-football-extension-config.xml";

  @Rule
  public TestRule chain =
      RuleChain
          .outerRule(footballPort = new DynamicPort("footballPort"))
          .around(laLigaPort = new DynamicPort("laLigaPort"))
          .around(footballAddress = getFootballAddress())
          .around(laLigaAddress = getLaLigaAddress())
          .around(footballServiceServer = getFootballServiceServer())
          .around(laLigaServiceServer = getLaLigaServiceServer());

  public DynamicPort footballPort;

  public DynamicPort laLigaPort;

  public ExternalProcess footballServiceServer;

  public ExternalProcess laLigaServiceServer;

  public SystemProperty footballAddress;

  public SystemProperty laLigaAddress;

  // TODO MULE-16661 Use a Docker image instead of this
  // These need to run with a plain classloader to avoid conflicting libs in different artifacts in the Mule classloader
  // hierarchy, in order to avoid a LinkageError when running in JDK 11 or higher.
  public ExternalProcess getLaLigaServiceServer() {
    return new ExternalProcess(line -> line
        .contains("org.eclipse.jetty.server.ServerConnector: Started ServerConnector"),
                               "java", "-cp",
                               System.getProperty("soapHttpServerClasspath"),
                               "org.mule.service.soap.server.HttpServer",
                               "" + laLigaPort.getNumber(),
                               "org.mule.it.soap.connect.services.LaLigaService");
  }

  public ExternalProcess getFootballServiceServer() {
    return new ExternalProcess(line -> line
        .contains("org.eclipse.jetty.server.ServerConnector: Started ServerConnector"),
                               "java", "-cp",
                               System.getProperty("soapHttpServerClasspath"),
                               "org.mule.service.soap.server.HttpServer",
                               "" + footballPort.getNumber(),
                               "org.mule.it.soap.connect.services.FootballService");
  }

  public SystemProperty getFootballAddress() {
    return new SystemProperty("footballAddress", "http://localhost:" + footballPort.getNumber() + "/server");
  }

  public SystemProperty getLaLigaAddress() {
    return new SystemProperty("laLigaAddress", "http://localhost:" + laLigaPort.getNumber() + "/server");
  }

  @Override
  protected String getConfigFile() {
    return SOAP_CONFIG;
  }

  String getBodyXml(String tagName, String content) {
    String ns = "http://services.connect.soap.it.mule.org/";
    return String.format("<con:%s xmlns:con=\"%s\">%s</con:%s>", tagName, ns, content, tagName);
  }
}
