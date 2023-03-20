/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.exception.MuleException.INFO_LOCATION_KEY;
import static org.mule.runtime.api.exception.MuleException.INFO_SOURCE_XML_KEY;
import static org.mule.runtime.http.api.HttpConstants.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.mule.runtime.http.api.HttpConstants.Method.GET;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.http.api.HttpService;
import org.mule.runtime.http.api.domain.message.request.HttpRequest;
import org.mule.runtime.http.api.domain.message.response.HttpResponse;
import org.mule.service.http.TestHttpClient;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.test.AbstractIntegrationTestCase;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class ExceptionHandlingSourceTestCase extends AbstractIntegrationTestCase {

  @Rule
  public DynamicPort port = new DynamicPort("port");

  @Rule
  public TestHttpClient httpClient = new TestHttpClient.Builder(getService(HttpService.class)).build();

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/exceptions/exception-handling-source-test.xml";
  }

  @Before
  public void before() {
    OnErrorCounterProcessor.count.set(0);
  }

  @Test
  public void errorSendingResponse() throws Exception {
    HttpRequest request = HttpRequest.builder()
        .uri("http://localhost:" + port.getNumber() + "/errorSendingResponse")
        .method(GET).build();

    HttpResponse response = httpClient.send(request, RECEIVE_TIMEOUT, false, null);

    assertThat(response.getStatusCode(), is(INTERNAL_SERVER_ERROR.getStatusCode()));
    assertThat(OnErrorCounterProcessor.count.get(), is(1));
  }

  @Test
  public void errorSendingErrorResponse() throws Exception {
    HttpRequest request = HttpRequest.builder()
        .uri("http://localhost:" + port.getNumber() + "/errorSendingErrorResponse")
        .method(GET).build();

    HttpResponse response = httpClient.send(request, RECEIVE_TIMEOUT, false, null);

    assertThat(response.getStatusCode(), is(INTERNAL_SERVER_ERROR.getStatusCode()));
    assertThat(OnErrorCounterProcessor.count.get(), is(1));
  }

  @Test
  public void errorSendingThrownError() throws Exception {
    HttpRequest request = HttpRequest.builder()
        .uri("http://localhost:" + port.getNumber() + "/errorSendingThrownError")
        .method(GET).build();

    HttpResponse response = httpClient.send(request, RECEIVE_TIMEOUT, false, null);

    assertThat(response.getStatusCode(), is(INTERNAL_SERVER_ERROR.getStatusCode()));
    assertThat(OnErrorCounterProcessor.count.get(), is(0));
  }

  @Test
  public void errorSendingErrorFromHandler() throws Exception {
    HttpRequest request = HttpRequest.builder()
        .uri("http://localhost:" + port.getNumber() + "/errorSendingErrorFromHandler")
        .method(GET).build();

    HttpResponse response = httpClient.send(request, RECEIVE_TIMEOUT, false, null);

    assertThat(response.getStatusCode(), is(INTERNAL_SERVER_ERROR.getStatusCode()));
    assertThat(OnErrorCounterProcessor.count.get(), is(1));
  }

  @Test
  public void errorSendingPropagatedError() throws Exception {
    HttpRequest request = HttpRequest.builder()
        .uri("http://localhost:" + port.getNumber() + "/errorSendingPropagatedError")
        .method(GET).build();

    HttpResponse response = httpClient.send(request, RECEIVE_TIMEOUT, false, null);

    assertThat(response.getStatusCode(), is(INTERNAL_SERVER_ERROR.getStatusCode()));
    assertThat(OnErrorCounterProcessor.count.get(), is(1));
  }

  @Test
  public void errorInHandlerHasOrigin() throws Exception {
    MuleException exception = (MuleException) flowRunner("errorInHandlerHasOrigin").runExpectingException();

    assertThat((String) exception.getInfo().get(INFO_LOCATION_KEY), containsString("errorHandler/0/processors/0"));
    assertThat((String) exception.getInfo().get(INFO_SOURCE_XML_KEY), containsString("APP:EXPECTED_INSIDE_HANDLER"));
  }

  public static class OnErrorCounterProcessor extends AbstractComponent implements Processor {

    private static AtomicInteger count = new AtomicInteger(0);

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      count.incrementAndGet();
      return event;
    }
  }
}
