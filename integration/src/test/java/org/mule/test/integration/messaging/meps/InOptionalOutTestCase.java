/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.integration.messaging.meps;

import static java.lang.String.format;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mule.runtime.core.api.util.StringUtils.EMPTY;
import static org.mule.runtime.http.api.HttpConstants.Method.GET;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.http.api.HttpService;
import org.mule.runtime.http.api.domain.entity.ByteArrayHttpEntity;
import org.mule.runtime.http.api.domain.entity.HttpEntity;
import org.mule.runtime.http.api.domain.message.request.HttpRequest;
import org.mule.service.http.TestHttpClient;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Rule;
import org.junit.Test;

public class InOptionalOutTestCase extends AbstractIntegrationTestCase {

  public static final long TIMEOUT = 3000;

  @Rule
  public DynamicPort port = new DynamicPort("port1");

  @Rule
  public TestHttpClient httpClient = new TestHttpClient.Builder(getService(HttpService.class)).build();

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/messaging/meps/pattern_In-Optional-Out-flow.xml";
  }

  @Test
  public void testExchange() throws Exception {
    String listenerUrl = format("http://localhost:%s/", port.getNumber());
    HttpRequest request = HttpRequest.builder().uri(listenerUrl).method(GET)
        .entity(new ByteArrayHttpEntity("some data".getBytes())).build();
    HttpEntity responseEntity = httpClient.send(request, RECEIVE_TIMEOUT, false, null).getEntity();

    assertNotNull(responseEntity);
    assertEquals(EMPTY, IOUtils.toString(responseEntity.getContent()));

    request = HttpRequest.builder().uri(listenerUrl).method(GET).addHeader("foo", "bar")
        .entity(new ByteArrayHttpEntity("some data".getBytes())).build();
    responseEntity = httpClient.send(request, RECEIVE_TIMEOUT, false, null).getEntity();
    assertNotNull(responseEntity);
    assertEquals("foo header received", IOUtils.toString(responseEntity.getContent()));
  }
}
