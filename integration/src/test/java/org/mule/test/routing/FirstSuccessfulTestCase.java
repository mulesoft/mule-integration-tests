/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.routing;

import static org.mule.test.allure.AllureConstants.RoutersFeature.FirstSuccessfulStory.FIRST_SUCCESSFUL;
import static org.mule.test.allure.AllureConstants.RoutersFeature.ROUTERS;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import org.mule.extension.validation.api.ValidationException;
import org.mule.functional.api.exception.ExpectedError;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.privileged.routing.CompositeRoutingException;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.tests.api.TestQueueManager;

import javax.inject.Inject;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import org.junit.Rule;
import org.junit.Test;

@Feature(ROUTERS)
@Story(FIRST_SUCCESSFUL)
public class FirstSuccessfulTestCase extends AbstractIntegrationTestCase {

  @Inject
  private TestQueueManager queueManager;

  @Rule
  public ExpectedError expectedError = ExpectedError.none();

  @Override
  protected String getConfigFile() {
    return "first-successful-test.xml";
  }

  @Test
  public void firstRouteWorks() throws Exception {
    Message response = flowRunner("test-router").withPayload("XYZ").run().getMessage();
    assertThat(getPayloadAsString(response), is("XYZ is a string"));
  }

  @Test
  public void secondRouteWorks() throws Exception {
    Message response = flowRunner("test-router").withPayload(Integer.valueOf(9)).run().getMessage();
    assertThat(getPayloadAsString(response), is("9 is a number"));

    response = flowRunner("test-router").withPayload(Long.valueOf(42)).run().getMessage();
    assertThat(getPayloadAsString(response), is("42 is a number"));
  }

  @Test
  public void allRoutesFail() throws Exception {
    expectedError.expectCause(instanceOf(ValidationException.class));
    expectedError.expectErrorType("VALIDATION", "INVALID_BOOLEAN");
    flowRunner("test-router").withPayload(Boolean.TRUE).run().getMessage();
  }

  @Test
  public void nestedFirstSuccessful() throws Exception {
    CoreEvent event = flowRunner("nestedFirstSuccessful").withPayload(TEST_PAYLOAD).run();
    assertThat(event.getMessage().getPayload().getValue(), is(TEST_PAYLOAD + " hello"));
  }

  @Test
  public void oneWayEndpoints() throws Exception {
    flowRunner("withOneWayEndpoints").withPayload(TEST_MESSAGE).run();

    Message response = queueManager.read("WithOneWayEndpoints.out", RECEIVE_TIMEOUT, MILLISECONDS).getMessage();
    assertThat(response, is(notNullValue()));
    assertThat(response.getPayload().getValue(), is(TEST_MESSAGE));
  }

  @Test
  public void insideErrorHandler() throws Exception {
    CoreEvent event = flowRunner("firstSuccessfulInErrorHandler").withPayload(TEST_PAYLOAD).run();
    assertThat(event.getMessage().getPayload().getValue(), is("La pelota no se mancha"));
  }

  @Test
  public void insideErrorHandlerWithError() throws Exception {
    CoreEvent event = flowRunner("firstSuccessfulInErrorHandlerWithError").withPayload(TEST_PAYLOAD).run();
    assertThat(event.getMessage().getPayload().getValue(), is("Yo me equivoque y pague"));
  }

  @Test
  public void insideErrorHandlerFailing() throws Exception {
    expectedError.expectCause(instanceOf(ValidationException.class));
    expectedError.expectErrorType("VALIDATION", "INVALID_BOOLEAN");
    flowRunner("firstSuccessfulInErrorHandlerWithFailing").withPayload(Boolean.TRUE).run().getMessage();
  }

  @Test
  @Issue("MULE-19169")
  public void firstSuccessfulInsideParalleLForEach() throws Exception {
    CoreEvent event = flowRunner("firstSuccessfulInParallelForEach").run();
    assertThat(getPayloadAsString(event.getMessage()), is("Se te escapo la tortuga"));
    assertThat(event.getVariables().get("wasExecuted").getValue(), is("true"));
  }

  @Test
  @Issue("W-10619792")
  @Description("When a First Successful is followed by a Raise Error inside a Scatter Gather's route, should error.")
  public void firstSuccessfulAndRaiseErrorInsideScatterGather() throws Exception {
    expectedError.expectCause(instanceOf(CompositeRoutingException.class));
    expectedError.expectErrorType("MULE", "COMPOSITE_ROUTING");
    flowRunner("firstSuccessfulAndRaiseErrorInsideScatterGather").run();
  }

  @Test
  @Issue("W-12552091")
  @Description("When a First Successful invokes a flow that has a raise error, then it should still proceed to next route.")
  public void FirstSuccessfulToFlowWithError() throws Exception {
    CoreEvent result = flowRunner("FirstSuccessfulToFlowWithError").run();
    assertThat(getPayloadAsString(result.getMessage()), is("A Di Maria me lo resistian"));
  }

}
