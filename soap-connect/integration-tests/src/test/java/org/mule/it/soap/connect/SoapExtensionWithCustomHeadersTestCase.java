/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.it.soap.connect;

import static org.mule.service.soap.SoapTestUtils.assertSimilarXml;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

public class SoapExtensionWithCustomHeadersTestCase extends AbstractSimpleServiceFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "custom-headers.xml";
  }

  @Test
  public void useServiceProviderWithCustomHeaders() throws Exception {
    Message m = flowRunner("customHeaders").keepStreamsOpen().run().getMessage();
    String result = IOUtils.toString(((CursorStreamProvider) m.getPayload().getValue()).openCursor());
    assertSimilarXml("<ns2:noParamsWithHeaderResponse xmlns:ns2=\"http://service.soap.service.mule.org/\">"
        + "<text>OP=noParamsWithHeader</text>"
        + "</ns2:noParamsWithHeaderResponse>", result);
  }

}
