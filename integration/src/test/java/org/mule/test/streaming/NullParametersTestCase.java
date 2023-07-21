/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.streaming;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.http.api.HttpConstants.Method.GET;
import static org.mule.test.allure.AllureConstants.StreamingFeature.STREAMING;

import org.mule.runtime.http.api.HttpService;
import org.mule.runtime.http.api.domain.entity.ByteArrayHttpEntity;
import org.mule.runtime.http.api.domain.message.request.HttpRequest;
import org.mule.runtime.http.api.domain.message.response.HttpResponse;
import org.mule.service.http.TestHttpClient;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.test.AbstractIntegrationTestCase;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.junit.Rule;
import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;

@Feature(STREAMING)
public class NullParametersTestCase extends AbstractIntegrationTestCase {

  @Rule
  public DynamicPort port = new DynamicPort("port");

  @Override
  protected String getConfigFile() {
    return "org/mule/streaming/null-parameters-config.xml";
  }

  @Rule
  public TestHttpClient httpClient = new TestHttpClient.Builder(getService(HttpService.class)).build();

  @Test
  public void nullOperationParam() throws Exception {
    flowRunner("operationExplicit").run();
  }

  @Test
  public void varNotExistsOperationParam() throws Exception {
    flowRunner("operationVar").run();
  }

  @Test
  @Issue("MULE-18876")
  public void nullSourceResponseParam() throws IOException, TimeoutException {
    HttpRequest request = HttpRequest.builder().uri("http://localhost:" + port.getNumber() + "/testExplicit")
        .method(GET).entity(new ByteArrayHttpEntity("test".getBytes())).build();
    final HttpResponse response = httpClient.send(request, DEFAULT_TEST_TIMEOUT_SECS, false, null);
    assertThat(response.getStatusCode(), is(200));

  }

  @Test
  @Issue("MULE-18876")
  public void varNotExistsSourceResponseParam() throws IOException, TimeoutException {
    HttpRequest request = HttpRequest.builder().uri("http://localhost:" + port.getNumber() + "/testVar")
        .method(GET).entity(new ByteArrayHttpEntity("test".getBytes())).build();
    final HttpResponse response = httpClient.send(request, DEFAULT_TEST_TIMEOUT_SECS, false, null);
    assertThat(response.getStatusCode(), is(200));
  }
}
