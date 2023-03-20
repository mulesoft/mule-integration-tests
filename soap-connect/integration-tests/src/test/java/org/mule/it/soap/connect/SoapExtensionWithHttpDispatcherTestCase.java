/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.it.soap.connect;

import static org.mule.service.soap.SoapTestUtils.assertSimilarXml;

import org.mule.runtime.api.message.Message;

import org.junit.Test;

public class SoapExtensionWithHttpDispatcherTestCase extends AbstractSimpleServiceFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "http-dispatcher.xml";
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
    assertSimilarXml("<ns2:noParamsResponse xmlns:ns2=\"http://service.soap.service.mule.org/\">"
        + "<text>response</text>"
        + "</ns2:noParamsResponse>", ((String) m.getPayload().getValue()));
  }

  // TODO MULE-17934 remove this
  @Override
  protected boolean isGracefulShutdown() {
    return true;
  }
}
