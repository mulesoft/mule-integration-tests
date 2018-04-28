/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mule.functional.api.exception.ExpectedError.none;
import static org.mule.functional.junit4.matchers.MessageMatchers.hasPayload;
import static org.mule.functional.junit4.matchers.ThrowableCauseMatcher.hasCause;
import static org.mule.runtime.http.api.HttpConstants.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.mule.runtime.http.api.HttpConstants.Method.POST;
import static org.mule.tck.junit4.matcher.EventMatcher.hasMessage;
import static org.mule.tck.junit4.matcher.EventMatcher.hasVariables;
import static org.mule.tck.junit4.matcher.HasClassInHierarchy.withClassName;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ERROR_HANDLING;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.mule.runtime.api.metadata.MediaType.TEXT;

import org.mule.functional.api.component.TestConnectorQueueHandler;
import org.mule.functional.api.exception.ExpectedError;
import org.mule.functional.api.exception.FunctionalTestException;
import org.mule.runtime.api.event.Event;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleFatalException;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.util.concurrent.Latch;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.http.api.HttpService;
import org.mule.runtime.http.api.domain.entity.ByteArrayHttpEntity;
import org.mule.runtime.http.api.domain.message.request.HttpRequest;
import org.mule.runtime.http.api.domain.message.response.HttpResponse;
import org.mule.service.http.TestHttpClient;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.test.AbstractIntegrationTestCase;

import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(ERROR_HANDLING)
@Story("On Error Propagate")
public class OnErrorPropagateTestCase extends AbstractIntegrationTestCase {

  private TestConnectorQueueHandler queueHandler;

  @Rule
  public ExpectedError expectedError = none();

  @Rule
  public DynamicPort port = new DynamicPort("port");

  @Rule
  public TestHttpClient httpClient = new TestHttpClient.Builder(getService(HttpService.class)).build();

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/exceptions/on-error-propagate-use-case-config.xml";
  }

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();
    queueHandler = new TestConnectorQueueHandler(registry);
  }

  @Test
  public void typeMatch() throws Exception {
    verifyFlow("onErrorPropagateTypeMatch");
    Event customPath = queueHandler.read("custom1", RECEIVE_TIMEOUT);
    assertThat(customPath, is(nullValue()));
    Event anyPath = queueHandler.read("any1", RECEIVE_TIMEOUT);
    assertThat(anyPath, is(nullValue()));
  }

  @Test
  public void typeMatchAny() throws Exception {
    verifyFlow("onErrorPropagateTypeMatchAny");
    Event customPath = queueHandler.read("custom2", RECEIVE_TIMEOUT);
    assertThat(customPath, is(nullValue()));
  }

  @Test
  public void typeMatchSeveral() throws Exception {
    verifyFlow("onErrorPropagateTypeMatchSeveral", true);
    Event anyPath = queueHandler.read("any", RECEIVE_TIMEOUT);
    assertThat(anyPath, is(nullValue()));
    verifyFlow("onErrorPropagateTypeMatchSeveral", false);
    anyPath = queueHandler.read("any", RECEIVE_TIMEOUT);
    assertThat(anyPath, is(nullValue()));
  }

  @Test
  public void propagateErrorAndMessage() throws Exception {
    expectedError.expectCause(withClassName("org.mule.runtime.api.exception.DefaultMuleException"));
    Matcher hasEntry =
        hasEntry("myVar",
                 new TypedValue<>("aValue",
                                  DataType.builder().type(String.class).mediaType(TEXT).charset(UTF_8)
                                      .build()));
    expectedError.expectEvent(allOf(hasVariables(hasEntry), hasMessage(hasPayload(equalTo("propagated")))));

    flowRunner("onErrorPropagateMessage").run();
  }

  @Test
  public void onErrorPropagateFailure() throws Exception {
    expectedError.expectCause(instanceOf(MuleFatalException.class));
    expectedError.expectCause(hasCause(instanceOf(NoClassDefFoundError.class)));
    flowRunner("failingHandler").run();
  }

  @Test
  public void handlesSourceErrors() throws Exception {
    HttpRequest request = HttpRequest.builder().uri(getUrl()).method(POST)
        .entity(new ByteArrayHttpEntity(TEST_MESSAGE.getBytes())).build();
    final HttpResponse response = httpClient.send(request, RECEIVE_TIMEOUT, false, null);

    assertThat(response.getStatusCode(), is(INTERNAL_SERVER_ERROR.getStatusCode()));
    assertThat(queueHandler.read("out", RECEIVE_TIMEOUT).getMessage(), hasPayload(equalTo("Test Message hey")));
  }

  private String getUrl() {
    return format("http://localhost:%s/sourceError", port.getNumber());
  }

  private void verifyFlow(String flowName, Object payload) throws InterruptedException {
    try {
      flowRunner(flowName).withPayload(payload).dispatch();
    } catch (Exception e) {
      assertThat(e.getCause(), is(instanceOf(FunctionalTestException.class)));
      if (!CallMessageProcessor.latch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS)) {
        fail("custom message processor wasn't call");
      }
    }
  }

  private void verifyFlow(String flowName) throws InterruptedException {
    verifyFlow(flowName, TEST_MESSAGE);
  }

  public static class ErrorProcessor implements Processor {

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      throw new NoClassDefFoundError("Test error");
    }

  }

  public static class CallMessageProcessor implements Processor {

    public static Latch latch = new Latch();

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      latch.release();
      return event;
    }
  }

}
