/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.usecases.sync;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.http.api.HttpConstants.Method.POST;
import static org.mule.tck.processor.FlowAssert.verify;

import org.mule.functional.api.component.TestConnectorQueueHandler;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.util.MultiMap;
import org.mule.runtime.http.api.HttpService;
import org.mule.runtime.http.api.domain.entity.ByteArrayHttpEntity;
import org.mule.runtime.http.api.domain.message.request.HttpRequest;
import org.mule.service.http.TestHttpClient;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

@Ignore("MULE-19149")
public class HttpJmsBridgeTestCase extends AbstractIntegrationTestCase {

  @Rule
  public DynamicPort httpPort = new DynamicPort("port");

  @Rule
  public TestHttpClient httpClient = new TestHttpClient.Builder(getService(HttpService.class)).build();

  @Override
  protected String getConfigFile() {
    return "org/mule/test/usecases/sync/http-jms-bridge-flow.xml";
  }

  @Test
  public void testBridge() throws Exception {
    String payload = "payload";

    MultiMap<String, String> headersMap = new MultiMap<>();
    final String customHeader = "X-Custom-Header";
    headersMap.put(customHeader, "value");

    HttpRequest request = HttpRequest.builder().uri(format("http://localhost:%d/in", httpPort.getNumber()))
        .entity(new ByteArrayHttpEntity(payload.getBytes())).headers(headersMap).method(POST).build();
    httpClient.send(request, RECEIVE_TIMEOUT, false, null);

    TestConnectorQueueHandler queueHandler = new TestConnectorQueueHandler(registry);
    Message msg = queueHandler.read("out", RECEIVE_TIMEOUT).getMessage();

    assertNotNull(msg);
    assertThat(getPayloadAsString(msg), is(payload));
    verify("bridge");
  }
}
