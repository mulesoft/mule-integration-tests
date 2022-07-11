/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.routing;

import static java.lang.System.lineSeparator;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.rules.ExpectedException.none;
import static org.mule.functional.junit4.matchers.ThrowableMessageMatcher.hasMessage;
import static org.mule.runtime.api.metadata.MediaType.ANY;
import static org.mule.runtime.api.metadata.MediaType.JSON;
import static org.mule.runtime.api.metadata.MediaType.TEXT;
import static org.mule.tck.junit4.matcher.HasClassInHierarchy.withClassName;
import static org.mule.test.allure.AllureConstants.RoutersFeature.ROUTERS;
import static org.mule.test.allure.AllureConstants.RoutersFeature.ScatterGatherStory.SCATTER_GATHER;
import static org.mule.test.routing.ThreadCaptor.getCapturedThreads;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.exception.ComposedErrorException;
import org.mule.runtime.api.exception.DefaultMuleException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.util.concurrent.Latch;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.expression.ExpressionRuntimeException;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.AbstractIntegrationTestCase;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;

@Feature(ROUTERS)
@Story(SCATTER_GATHER)
public class ScatterGatherRouterTestCase extends AbstractIntegrationTestCase {

  private static final String EXCEPTION_MESSAGE_TITLE_PREFIX = "Error(s) were found for route(s):" + lineSeparator();

  @Rule
  public ExpectedException expectedException = none();

  @Rule
  public SystemProperty timeout = new SystemProperty("scatterGather.timeout", "" + RECEIVE_TIMEOUT);

  @Override
  protected String getConfigFile() {
    return "routers/scatter-gather-test.xml";
  }

  @Test
  @Description("Minimal configuration with default collect-map strategy.")
  public void minimalConfiguration() throws Exception {
    flowRunner("minimalConfig").withPayload("foo").run();
  }

  @Test
  @Description("Minimal configuration with default collect-map strategy and target configured.")
  public void minimalConfigurationTarget() throws Exception {
    flowRunner("minimalConfigTarget").run();
  }

  @Test
  @Description("Minimal configuration with default collect-map strategy and target configured with targetType Message.")
  public void minimalConfigurationTargetMessage() throws Exception {
    flowRunner("minimalConfigTargetMessage").run();
  }

  @Test
  @Description("Minimal configuration with default with collect-list strategy configured.")
  public void minimalConfigurationCollectList() throws Exception {
    flowRunner("minimalConfigCollectList").run();
  }

  @Test
  @Description("Router request fails with runtime exception is payload is consumable.")
  public void consumablePayload() throws Exception {
    expectedException.expectCause(instanceOf(MuleRuntimeException.class));
    expectedException.expectCause(hasMessage(startsWith("Cannot copy message with a stream payload")));
    flowRunner("minimalConfig").withPayload(new ByteArrayInputStream("hello world".getBytes())).run();
  }

  @Test
  @Description("Router times out if routes take longer than the timeout configured to complete.")
  public void timeout() throws Exception {
    expectedException.expectCause(withClassName("org.mule.runtime.core.privileged.routing.CompositeRoutingException"));
    flowRunner("timeout").run();
  }

  @Test
  @Description("An error in a route results in a CompositeRoutingException containing details of exceptions.")
  public void routeWithException() throws Exception {
    assertRouteException("routeWithException", EXCEPTION_MESSAGE_TITLE_PREFIX
        + "\tRoute 1: org.mule.runtime.api.exception.DefaultMuleException: An error occurred.",
                         DefaultMuleException.class);
  }

  @Test
  @Description("An error in a route results in a CompositeRoutingException containing details of exceptions.")
  public void routeWithExceptionWithMessage() throws Exception {
    assertRouteException("routeWithExceptionWithMessage",
                         EXCEPTION_MESSAGE_TITLE_PREFIX
                             + "\tRoute 1: org.mule.runtime.api.exception.DefaultMuleException: I'm a message",
                         DefaultMuleException.class);
  }

  @Test
  @Description("An error in a route results in a CompositeRoutingException containing details of exceptions.")
  public void routeWithNonMuleException() throws Exception {
    assertRouteException("routeWithNonMuleException",
                         EXCEPTION_MESSAGE_TITLE_PREFIX + "\tRoute 1: java.lang.NullPointerException: nonMule",
                         NullPointerException.class);
  }

  @Test
  @Description("An error in a route results in a CompositeRoutingException containing details of exceptions.")
  public void routeWithExpressionException() throws Exception {
    assertRouteException("routeWithExpressionException",
                         message -> assertThat(message, both(containsString(EXCEPTION_MESSAGE_TITLE_PREFIX)).and(
                                                                                                                 containsString("1: org.mule.runtime.core.api.expression.ExpressionRuntimeException: \"Script 'invalidExpr' has errors:"))),
                         ExpressionRuntimeException.class);
  }

  @Test
  @Description("An error in a route when executing sequentially results in a CompositeRoutingException containing details of exceptions.")
  public void routeWithExceptionInSequentialProcessing() throws Exception {
    assertRouteException("routeWithExceptionInSequentialProcessing",
                         EXCEPTION_MESSAGE_TITLE_PREFIX
                             + "\tRoute 1: org.mule.runtime.api.exception.DefaultMuleException: An error occurred.",
                         DefaultMuleException.class);
  }

  private void assertRouteException(String flow, String exceptionMessageStart, Class exceptionType) throws Exception {
    assertRouteException(flow, message -> assertThat(message, startsWith(exceptionMessageStart)), exceptionType);
  }

  private void assertRouteException(String flow, Consumer<String> exceptionMessageMatcher, Class exceptionType) throws Exception {
    try {
      flowRunner(flow).run();
      fail("Was expecting a failure");
    } catch (Exception e) {
      assertThat(e.getCause(), withClassName("org.mule.runtime.core.privileged.routing.CompositeRoutingException"));

      Throwable compositeRoutingException = e.getCause();
      exceptionMessageMatcher.accept(compositeRoutingException.getMessage());

      List<org.mule.runtime.api.message.Error> exceptions = ((ComposedErrorException) compositeRoutingException).getErrors();
      assertThat(exceptions, hasSize(1));
      assertThat(exceptions.get(0).getCause(), instanceOf(exceptionType));
    }
  }

  @Test
  @Description("Only a single thread is used to process all routes when configured with maxConcurrency=1.")
  public void sequentialProcessing() throws Exception {
    flowRunner("sequentialProcessing").withVariable("latch", new Latch()).run();
    assertThat(getCapturedThreads(), hasSize(1));
  }

  @Test
  @Description("Only a single thread is used to process all routes when a transaction is active.")
  public void withinTransaction() throws Exception {
    flowRunner("withinTransaction").withVariable("latch", new Latch()).run();
    assertThat(getCapturedThreads(), hasSize(1));
  }

  @Test
  @Description("The result of all route failures and results are available via errorMessage in error-handler..")
  public void errorHandler() throws Exception {
    flowRunner("errorHandler").run();
  }

  @Test
  @Description("Variables set before route are conserved after router. Variables set in routes are merged and available after router.")
  public void variables() throws Exception {
    flowRunner("variables").run();
  }

  @Test
  @Description("Validates that scatter-gather can be used correctly within an error handler")
  public void scatterGatherInsideErrorHandler() throws Exception {
    CoreEvent event = flowRunner("scatterGatherInsideErrorHandler").run();
    assertThat(event.getMessage().getPayload().getValue(), is("hello"));
  }

  @Test
  @Description("Validates that if a route of a scatter-gather within an error handler fails, then only that route will have an error")
  public void scatterGatherInsideErrorHandlerThrowsError() throws Exception {
    CoreEvent event = flowRunner("scatterGatherInsideErrorHandlerThrowsError").run();
    assertThat(event.getMessage().getPayload().getValue(), is("hello"));
  }

  @Test
  @Issue("MULE-18154")
  @Description("Validates that an error handler in a scatter-gather route can be used correctly")
  public void errorHandlerInsideScatterGather() throws Exception {
    flowRunner("errorHandlerInsideScatterGather").run();
  }


  @Description("By default routes are run concurrently and multiple threads are used.")
  public void concurrent() throws Exception {
    flowRunner("concurrent").withVariable("latch", new Latch()).run();
    assertThat(getCapturedThreads(), hasSize(3));
  }

  @Test
  @Description("The resulting Map<String, Message> result maintains the correct data-type for each Message.")
  public void returnsCorrectDataType() throws Exception {
    Message response = flowRunner("dataType").withMediaType(JSON).run().getMessage();
    assertThat(response.getPayload().getValue(), is(Matchers.instanceOf(Map.class)));
    Map<String, Message> messageList = (Map<String, Message>) response.getPayload().getValue();
    assertThat(messageList.size(), is(3));
    assertThat(messageList.get("0").getPayload().getDataType().getMediaType(), is(TEXT));
    assertThat(messageList.get("1").getPayload().getDataType().getMediaType(), is(ANY));
    assertThat(messageList.get("2").getPayload().getDataType().getMediaType(), is(ANY));
  }

  @Test
  @Description("The resulting Map<String, Message> is iterable in the same order as the defined routes.")
  @Issue("MULE-18040")
  public void resultsInOrder() throws Exception {
    Message response = flowRunner("resultsInOrder").run().getMessage();

    assertThat(response.getPayload().getValue(), is(Matchers.instanceOf(Map.class)));
    Map<String, Message> messageList = (Map<String, Message>) response.getPayload().getValue();
    assertThat(messageList.size(), is(12));
    assertThat(messageList.values().stream().map(m -> m.getPayload().getValue()).collect(toList()),
               is(asList("A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L")));
  }

  @Test
  @Issue("MULE-18227")
  @Description("Check that parallel execution routes do not cause race conditions when handling SdkInternalContext")
  public void foreachWithinScatterGatherWithSdkOperation() throws Exception {
    flowRunner("foreachWithinScatterGatherWithSdkOperation").run();
  }

  public static final class ThrowNpeProcessor extends AbstractComponent implements Processor {

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      throw new NullPointerException("nonMule");
    }
  }
}
