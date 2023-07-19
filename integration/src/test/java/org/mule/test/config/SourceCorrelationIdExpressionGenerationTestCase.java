/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.config;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import org.junit.Rule;
import org.junit.Test;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.http.api.HttpService;
import org.mule.runtime.http.api.client.HttpRequestOptions;
import org.mule.runtime.http.api.domain.message.request.HttpRequest;
import org.mule.runtime.http.api.domain.message.response.HttpResponse;
import org.mule.service.http.TestHttpClient;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.test.AbstractIntegrationTestCase;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.runtime.http.api.HttpConstants.Method.GET;
import static org.mule.test.allure.AllureConstants.CorrelationIdFeature.CORRELATION_ID;
import static org.mule.test.allure.AllureConstants.CorrelationIdFeature.CorrelationIdOnSourcesStory.CORRELATION_ID_ON_SOURCES;

@Issue("MULE-18770")
@Feature(CORRELATION_ID)
@Story(CORRELATION_ID_ON_SOURCES)
public class SourceCorrelationIdExpressionGenerationTestCase extends AbstractIntegrationTestCase {

  private static final String EXPECTED_CORRELATION_ID_FORMAT = "^.{4}\\*doge$";

  @Rule
  public DynamicPort listenPort = new DynamicPort("port");

  @Rule
  public TestHttpClient httpClient = new TestHttpClient.Builder(getService(HttpService.class)).build();

  @Override
  protected String getConfigFile() {
    return "org/mule/test/config/correlation-id/expression-generation.xml";
  }

  @Test
  public void execute() throws Exception {
    HttpRequest request = HttpRequest.builder().uri("http://localhost:" + listenPort.getNumber() + "/test").method(GET).build();
    HttpResponse response = httpClient.send(request, HttpRequestOptions.builder().responseTimeout(10000).build());
    assertThat(response.getStatusCode(), is(200));
    String result = IOUtils.toString(response.getEntity().getContent());
    assertThat(result.matches(EXPECTED_CORRELATION_ID_FORMAT), is(true));
  }

}

