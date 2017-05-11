/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.it.soap.connect;

import static org.mule.services.soap.SoapTestUtils.assertSimilarXml;
import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.services.soap.TestHttpSoapServer;
import org.mule.services.soap.service.Soap11Service;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;

import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;

public class TestSoapExtensionWithHttpDispatcher extends MuleArtifactFunctionalTestCase {

  @Rule
  public DynamicPort port = new DynamicPort("testPort");

  private TestHttpSoapServer server = new TestHttpSoapServer(port.getNumber(), new Soap11Service());

  @Rule
  public SystemProperty systemProperty = new SystemProperty("address", server.getDefaultAddress());


  @Override
  protected String getConfigFile() {
    return "http-dispatcher.xml";
  }

  @Override
  protected void doSetUp() throws Exception {
    server.init();
  }

  @Override
  protected void doTearDown() throws Exception {
    server.stop();
  }

  @Test
  public void withDefaultHttp() throws Exception {
    executeAndAssertSoapCall("withDefaultHttp");
  }

  @Test
  public void withConfigHttp() throws Exception {
    executeAndAssertSoapCall("withConfigHttp");
  }

  private void executeAndAssertSoapCall(String flow) throws Exception {
    Message m = flowRunner(flow).keepStreamsOpen().run().getMessage();
    String result = IOUtils.toString(((CursorStreamProvider) m.getPayload().getValue()).openCursor());
    assertSimilarXml(
                     "<ns2:noParamsResponse xmlns:ns2=\"http://service.soap.services.mule.org/\"><text>response</text></ns2:noParamsResponse>",
                     result);
  }
}
