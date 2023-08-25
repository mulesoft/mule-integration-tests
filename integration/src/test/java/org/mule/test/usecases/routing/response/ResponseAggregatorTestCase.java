/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.usecases.routing.response;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.http.api.HttpConstants.Method.POST;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.http.api.HttpService;
import org.mule.runtime.http.api.domain.entity.ByteArrayHttpEntity;
import org.mule.runtime.http.api.domain.message.request.HttpRequest;
import org.mule.runtime.http.api.domain.message.response.HttpResponse;
import org.mule.service.http.TestHttpClient;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Rule;
import org.junit.Test;

public class ResponseAggregatorTestCase extends AbstractIntegrationTestCase {

  @Rule
  public DynamicPort port = new DynamicPort("port1");

  @Rule
  public TestHttpClient httpClient = new TestHttpClient.Builder(getService(HttpService.class)).build();

  @Override
  protected String getConfigFile() {
    return "org/mule/test/usecases/routing/response/response-router-flow.xml";
  }

  @Test
  public void testSyncResponse() throws Exception {
    HttpRequest request = HttpRequest.builder().uri(format("http://localhost:%s", port.getNumber()))
        .entity(new ByteArrayHttpEntity("request".getBytes())).method(POST).build();

    HttpResponse response = httpClient.send(request, RECEIVE_TIMEOUT, false, null);

    String payload = IOUtils.toString(response.getEntity().getContent());
    assertThat(payload, is("Received: request"));
  }
}
