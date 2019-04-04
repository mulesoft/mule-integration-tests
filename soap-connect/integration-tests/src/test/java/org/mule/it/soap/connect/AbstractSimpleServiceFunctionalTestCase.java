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

import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Rule;

public abstract class AbstractSimpleServiceFunctionalTestCase extends MuleArtifactFunctionalTestCase {

  @Rule
  public DynamicPort port = new DynamicPort("testPort");

  @Rule
  public ExternalProcess server =
      new ExternalProcess("java", "-cp", System.getProperty("soapHttpServerClasspath"), "org.mule.service.soap.server.HttpServer",
                          "" + port.getNumber(), "org.mule.service.soap.service.Soap11Service");

  @Rule
  public SystemProperty address = new SystemProperty("address", "http://localhost:" + port.getNumber() + "/server");

  @Override
  protected void doSetUp() throws Exception {
    XMLUnit.setIgnoreWhitespace(true);
  }
}
