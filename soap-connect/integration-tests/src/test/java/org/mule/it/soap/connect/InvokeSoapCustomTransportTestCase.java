/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.it.soap.connect;

import static org.mule.service.soap.SoapTestUtils.assertSimilarXml;
import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.it.soap.connect.services.InterdimentionalCableService;
import org.mule.runtime.api.message.Message;
import org.mule.service.soap.server.HttpServer;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.ram.RickAndMortyExtension;

import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Rule;
import org.junit.Test;

public class InvokeSoapCustomTransportTestCase extends MuleArtifactFunctionalTestCase {

  @Rule
  public DynamicPort port = new DynamicPort("servicePort");

  private HttpServer server = new HttpServer(port.getNumber(), null, null, new InterdimentionalCableService());

  @Rule
  public SystemProperty address = new SystemProperty("serviceAddress", server.getDefaultAddress());

  @Override
  protected void doSetUp() throws Exception {
    XMLUnit.setIgnoreWhitespace(true);
  }

  @Override
  protected void doTearDown() throws Exception {
    super.doTearDown();
    server.stop();
  }

  @Override
  protected String getConfigFile() {
    return "soap-rick-and-morty-extension-config.xml";
  }

  @Test
  public void sendThroughCustomTransport() throws Exception {
    Message message = flowRunner("customTransport").keepStreamsOpen().run().getMessage();
    String response =
        "<con:ram xmlns:con=\"http://ram.test.mule.org\"><text>" + RickAndMortyExtension.RICKS_PHRASE + "</text></con:ram>";
    assertSimilarXml(response, (String) message.getPayload().getValue());
  }

  @Test
  public void sendThroughCustomTransportWithParams() throws Exception {
    Message message = flowRunner("customTransportWithParams").keepStreamsOpen().run().getMessage();
    String response = "<con:ram xmlns:con=\"http://ram.test.mule.org\"><text>CUSTOM RESPONSE</text></con:ram>";
    assertSimilarXml(response, (String) message.getPayload().getValue());
  }
}
