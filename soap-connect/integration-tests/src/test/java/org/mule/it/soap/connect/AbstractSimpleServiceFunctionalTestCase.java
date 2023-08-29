/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.it.soap.connect;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.ExternalProcess;
import org.mule.tck.junit4.rule.SystemProperty;

import org.custommonkey.xmlunit.XMLUnit;
import org.junit.ClassRule;
import org.junit.Rule;

public abstract class AbstractSimpleServiceFunctionalTestCase extends MuleArtifactFunctionalTestCase {

  @ClassRule
  public static DynamicPort port = new DynamicPort("testPort");

  // TODO MULE-16661 Use a Docker image instead of this
  // This needs to run with a plain classloader to avoid conflicting libs in different artifacts in the Mule classloader
  // hierarchy, in order to avoid a LinkageError when running in JDK 11 or higher.
  @ClassRule
  public static ExternalProcess server =
      new ExternalProcess(line -> line.contains("org.eclipse.jetty.server.ServerConnector: Started ServerConnector"),
                          "java", "-cp", System.getProperty("soapHttpServerClasspath"), "org.mule.service.soap.server.HttpServer",
                          "" + port.getNumber(), "org.mule.service.soap.service.Soap11Service");

  @Rule
  public SystemProperty address = new SystemProperty("address", "http://localhost:" + port.getNumber() + "/server");

  @Override
  protected void doSetUp() throws Exception {
    XMLUnit.setIgnoreWhitespace(true);
  }
}
