/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static java.lang.String.format;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.functional.api.exception.ExpectedError.none;
import static org.mule.runtime.core.api.exception.Errors.Identifiers.REDELIVERY_EXHAUSTED_ERROR_IDENTIFIER;
import static org.mule.runtime.http.api.HttpConstants.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.mule.runtime.http.api.HttpConstants.HttpStatus.SERVICE_UNAVAILABLE;
import static org.mule.runtime.http.api.HttpConstants.Method.POST;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ERROR_HANDLING;

import org.mule.functional.api.component.TestConnectorQueueHandler;
import org.mule.functional.api.exception.ExpectedError;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.http.api.HttpService;
import org.mule.runtime.http.api.domain.entity.ByteArrayHttpEntity;
import org.mule.runtime.http.api.domain.message.request.HttpRequest;
import org.mule.runtime.http.api.domain.message.response.HttpResponse;
import org.mule.service.http.TestHttpClient;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(ERROR_HANDLING)
@Story("Redelivery Exceeded")
public class RedeliveryExhaustedTestCase extends AbstractIntegrationTestCase {

  private static final int MAX_REDELIVERY_COUNT = 2;
  private static TestConnectorQueueHandler queueHandler;

  @Rule
  public ExpectedError expectedError = none();

  @Rule
  public DynamicPort port = new DynamicPort("port");

  @Rule
  public SystemProperty maxRedeliveryCount = new SystemProperty("maxRedeliveryCount", "" + MAX_REDELIVERY_COUNT);

  @Rule
  public TestHttpClient httpClient = new TestHttpClient.Builder(getService(HttpService.class)).build();

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/exceptions/redelivery-exhausted.xml";
  }

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();
    queueHandler = new TestConnectorQueueHandler(registry);
  }

  @Test
  @Description("Test that the required troubleshooting information is in the redelivery error.")
  public void exhaustRedelivery() throws IOException, TimeoutException {
    for (int i = 0; i < MAX_REDELIVERY_COUNT + 1; ++i) {
      assertThat(sendThroughHttp().getStatusCode(), is(INTERNAL_SERVER_ERROR.getStatusCode()));
    }

    final HttpResponse response = sendThroughHttp();

    assertThat(response.getStatusCode(), is(SERVICE_UNAVAILABLE.getStatusCode()));

    CoreEvent event = queueHandler.read("out", RECEIVE_TIMEOUT);

    Error error = (Error) event.getMessage().getPayload().getValue();

    assertThat(error.getErrorType().getIdentifier(), is(REDELIVERY_EXHAUSTED_ERROR_IDENTIFIER));
    assertThat(error.getChildErrors(), hasSize(MAX_REDELIVERY_COUNT + 1));

    for (Error childError : error.getChildErrors()) {
      assertThat(childError.getErrorType().getIdentifier(), is("ROUTING"));
    }
  }

  private HttpResponse sendThroughHttp() throws IOException, TimeoutException {
    HttpRequest request = HttpRequest.builder().uri(getUrl()).method(POST)
        .entity(new ByteArrayHttpEntity(TEST_MESSAGE.getBytes())).build();
    return httpClient.send(request, RECEIVE_TIMEOUT, false, null);
  }


  private String getUrl() {
    return format("http://localhost:%s/exhaustRedelivery", port.getNumber());
  }
}
