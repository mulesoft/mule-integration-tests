/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.integration.domain.http;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.http.api.HttpConstants.Method.GET;

import org.mule.functional.api.component.TestConnectorQueueHandler;
import org.mule.functional.junit4.DomainFunctionalTestCase;
import org.mule.runtime.http.api.domain.message.request.HttpRequest;
import org.mule.service.http.TestHttpClient;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

public class NotSharedHttpConnectorInDomain extends DomainFunctionalTestCase {

  private static final String APP = "app";

  @Rule
  public DynamicPort dynamicPort = new DynamicPort("port1");

  @Rule
  public TestHttpClient httpClient = new TestHttpClient.Builder().build();

  @Override
  protected String getDomainConfig() {
    return "domain/empty-domain-config.xml";
  }

  @Override
  public ApplicationConfig[] getConfigResources() {
    return new ApplicationConfig[] {new ApplicationConfig(APP, new String[] {"domain/http/http-not-shared-listener-config.xml"})};
  }

  @Test
  public void sendMessageToNotSharedConnectorInDomain() throws Exception {
    String url = format("http://localhost:%d/test", dynamicPort.getNumber());

    HttpRequest request = HttpRequest.builder().uri(url).method(GET).build();
    httpClient.send(request, DEFAULT_TEST_TIMEOUT_SECS, false, null);

    TestConnectorQueueHandler queueHandler = new TestConnectorQueueHandler(this.getInfrastructureForApp(APP).getRegistry());
    assertThat(queueHandler.read("in", 5000).getMessage(), is(notNullValue()));
  }
}
