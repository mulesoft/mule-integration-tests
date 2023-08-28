/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.streaming;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.http.api.HttpService;
import org.mule.runtime.http.api.domain.message.request.HttpRequest;
import org.mule.runtime.http.api.domain.message.response.HttpResponse;
import org.mule.service.http.TestHttpClient;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.test.AbstractIntegrationTestCase;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.junit.Rule;
import org.junit.Test;

public class SourceResponseTestCase extends AbstractIntegrationTestCase {

  @Rule
  public DynamicPort httpPort = new DynamicPort("httpPort");

  @Rule
  public TestHttpClient httpClient = new TestHttpClient.Builder(getService(HttpService.class)).build();

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/streaming/source-response-config.xml";
  }

  @Test
  public void responseParameterWithCursor() throws IOException, TimeoutException {
    HttpResponse httpResponse =
        httpClient.send(HttpRequest.builder().uri(String.format("http://localhost:%s/", httpPort.getNumber())).build());
    assertThat(httpResponse.getStatusCode(), is(200));
    String responsePayload = IOUtils.toString(httpResponse.getEntity().getContent());
    assertThat(responsePayload, containsString("<name>Foo</name>"));
    assertThat(responsePayload, containsString("<lastName>Bar</lastName>"));
  }

}
