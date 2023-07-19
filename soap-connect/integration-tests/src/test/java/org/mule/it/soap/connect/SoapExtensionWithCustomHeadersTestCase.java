/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.it.soap.connect;

import static org.mule.service.soap.SoapTestUtils.assertSimilarXml;

import static extension.org.mule.soap.it.Environment.PROD;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.mule.runtime.api.message.Message;

import org.junit.Ignore;
import org.junit.Test;

import extension.org.mule.soap.it.TestServiceProviderWithCustomHeaders;

public class SoapExtensionWithCustomHeadersTestCase extends AbstractSimpleServiceFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "custom-headers.xml";
  }

  @Test
  @Ignore("W-11747609")
  public void useServiceProviderWithCustomHeaders() throws Exception {
    Message m = flowRunner("customHeaders").keepStreamsOpen().run().getMessage();
    assertSimilarXml("<ns2:noParamsWithHeaderResponse xmlns:ns2=\"http://service.soap.service.mule.org/\">"
        + "<text>OP=noParamsWithHeader</text>"
        + "</ns2:noParamsWithHeaderResponse>", ((String) m.getPayload().getValue()));
  }

  @Test
  public void serviceProviderWithEnumParameter() throws Exception {
    assertThat(TestServiceProviderWithCustomHeaders.ENVIRONMENT, is(PROD));
  }
}
