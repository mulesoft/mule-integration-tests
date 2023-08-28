/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.routing;

import static org.mule.runtime.api.component.ComponentIdentifier.builder;
import static org.mule.runtime.api.util.MuleSystemProperties.PARALLEL_FOREACH_FLATTEN_MESSAGE_PROPERTY;
import static org.mule.runtime.core.api.error.Errors.ComponentIdentifiers.Handleable.EXPRESSION;
import static org.mule.runtime.core.api.error.Errors.ComponentIdentifiers.Handleable.UNKNOWN;
import static org.mule.tck.junit4.matcher.ErrorTypeMatcher.errorType;
import static org.mule.tck.junit4.matcher.HasClassInHierarchy.withClassName;
import static org.mule.test.allure.AllureConstants.RoutersFeature.ROUTERS;
import static org.mule.test.allure.AllureConstants.ScopeFeature.ParallelForEachStory.PARALLEL_FOR_EACH;
import static org.mule.test.routing.ThreadCaptor.getCapturedThreads;

import static java.lang.System.lineSeparator;
import static java.util.Arrays.asList;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.rules.ExpectedException.none;

import org.mule.functional.junit4.rules.HttpServerRule;
import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.exception.ComposedErrorException;
import org.mule.runtime.api.exception.DefaultMuleException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.util.concurrent.Latch;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.expression.ExpressionRuntimeException;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.AbstractIntegrationTestCase;

import java.util.List;
import java.util.function.Consumer;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;

@Feature(ROUTERS)
@Story(PARALLEL_FOR_EACH)
public class ParallelForEachTestCase extends AbstractIntegrationTestCase {

  private static final ComponentIdentifier EXPECTED =
      builder().namespace("APP").name("EXPECTED").build();

  private static final String EXCEPTION_MESSAGE_TITLE_PREFIX = "Error(s) were found for route(s):" + lineSeparator();
  private final String[] fruitList = new String[] {"apple", "banana", "orange"};

  @Rule
  public ExpectedException expectedException = none();

  @Rule
  public DynamicPort port = new DynamicPort("port");

  @Rule
  public HttpServerRule httpServerRules = new HttpServerRule("port");

  @Rule
  public SystemProperty parallelForeachFlattenMessage = new SystemProperty(PARALLEL_FOREACH_FLATTEN_MESSAGE_PROPERTY, "true");

  @Override
  protected String getConfigFile() {
    return "routers/parallel-foreach-config.xml";
  }

  @Test
  @Description("Minimal configuration with default collect-list strategy.")
  public void minimalConfiguration() throws Exception {
    flowRunner("minimalConfig").withPayload(fruitList).run();
  }

  @Test
  @Description("Minimal configuration with default collect-list strategy with nested list.")
  public void minimalConfigurationNested() throws Exception {
    flowRunner("minimalConfigNested")
        .withPayload(asList(new String[] {"1", "2", "3", "4"}, new String[] {"a", "b", "c", "d"},
                            new String[] {"i", "ii", "iii", "iv"}))
        .run();
  }

  @Test
  @Description("Minimal configuration with default collect-list strategy and custom collection expression")
  public void minimalConfigurationCollectionExpression() throws Exception {
    flowRunner("minimalConfigurationCollectionExpression").run();
  }

  @Test
  @Description("Minimal configuration with default collect-list strategy and target configured.")
  public void minimalConfigurationTarget() throws Exception {
    flowRunner("minimalConfigTarget").withPayload(fruitList).run();
  }

  @Test
  @Description("Minimal configuration with default collect-list strategy and target configured with targetType Message.")
  public void minimalConfigurationTargetMessage() throws Exception {
    flowRunner("minimalConfigTargetMessage").withPayload(fruitList).run();
  }

  @Test
  @Description("Router times out if routes take longer than the timeout configured to complete.")
  public void timeout() throws Exception {
    expectedException.expectCause(withClassName("org.mule.runtime.core.privileged.routing.CompositeRoutingException"));
    flowRunner("timeout").withPayload(fruitList).run();
  }

  @Test
  @Description("An error in a route results in a CompositeRoutingException containing details of exceptions.")
  public void routeWithException() {
    assertRouteException("routeWithException", EXCEPTION_MESSAGE_TITLE_PREFIX
        + "\tRoute 1: org.mule.runtime.api.exception.DefaultMuleException: An error occurred.",
                         DefaultMuleException.class, EXPECTED);
  }

  @Test
  @Description("An error in a route results in a CompositeRoutingException containing details of exceptions.")
  public void routeWithExceptionWithMessage() {
    assertRouteException("routeWithExceptionWithMessage",
                         EXCEPTION_MESSAGE_TITLE_PREFIX
                             + "\tRoute 1: org.mule.runtime.api.exception.DefaultMuleException: I'm a message",
                         DefaultMuleException.class, EXPECTED);
  }

  @Test
  @Description("An error in a route results in a CompositeRoutingException containing details of exceptions.")
  public void routeWithNonMuleException() {
    assertRouteException("routeWithNonMuleException",
                         EXCEPTION_MESSAGE_TITLE_PREFIX + "\tRoute 1: java.lang.NullPointerException: nonMule",
                         NullPointerException.class, UNKNOWN);
  }

  @Test
  @Description("An error in a route results in a CompositeRoutingException containing details of exceptions.")
  public void routeWithExpressionException() {
    assertRouteException("routeWithExpressionException",
                         message -> assertThat(message, both(containsString(EXCEPTION_MESSAGE_TITLE_PREFIX)).and(
                                                                                                                 containsString("1: org.mule.runtime.core.api.expression.ExpressionRuntimeException: \"Script 'invalidExpr' has errors:"))),
                         ExpressionRuntimeException.class,
                         EXPRESSION);
  }

  @Test
  @Description("An error in a route when executing sequentially results in a CompositeRoutingException containing details of exceptions.")
  public void routeWithExceptionInSequentialProcessing() {
    assertRouteException("routeWithExceptionInSequentialProcessing",
                         EXCEPTION_MESSAGE_TITLE_PREFIX
                             + "\tRoute 1: org.mule.runtime.api.exception.DefaultMuleException: An error occurred.",
                         DefaultMuleException.class, EXPECTED);
  }

  private void assertRouteException(String flow,
                                    String exceptionMessageStart,
                                    Class exceptionType,
                                    ComponentIdentifier errorType) {
    assertRouteException(flow, message -> assertThat(message, startsWith(exceptionMessageStart)), exceptionType, errorType);
  }

  private void assertRouteException(String flow,
                                    Consumer<String> exceptionMessageMatcher,
                                    Class exceptionType,
                                    ComponentIdentifier errorType) {
    try {
      flowRunner(flow).withPayload(fruitList).run();
      fail("Was expecting a failure");
    } catch (Exception e) {
      assertThat(e.getCause(), withClassName("org.mule.runtime.core.privileged.routing.CompositeRoutingException"));

      Throwable compositeRoutingException = e.getCause();
      exceptionMessageMatcher.accept(compositeRoutingException.getMessage());

      List<org.mule.runtime.api.message.Error> exceptions = ((ComposedErrorException) compositeRoutingException).getErrors();
      assertThat(exceptions, hasSize(1));
      assertThat(exceptions.get(0).getErrorType(), errorType(errorType));
      assertThat(exceptions.get(0).getCause(), instanceOf(exceptionType));
    }
  }

  @Test
  @Description("Only a single thread is used to process all routes when configured with maxConcurrency=1.")
  public void sequentialProcessing() throws Exception {
    flowRunner("sequentialProcessing").withPayload(fruitList).withVariable("latch", new Latch()).run();
    assertThat(getCapturedThreads(), hasSize(1));
  }

  @Test
  @Description("The result of all route failures and results are available via errorMessage in error-handler..")
  public void errorHandler() throws Exception {
    flowRunner("errorHandler").withPayload(fruitList).run();
  }

  @Test
  @Description("Variables set before route are conserved after router. Variables set in routes are merged and available after router.")
  public void variables() throws Exception {
    flowRunner("variables").withPayload(fruitList).run();
  }

  @Test
  @Description("Validates that parallel foreach can be used correctly within an error handler")
  public void parallelForEachInErrorHandler() throws Exception {
    CoreEvent event = flowRunner("parallelForEachInErrorHandler").run();
    assertThat(event.getMessage().getPayload().getValue(), is("hello"));
  }

  @Test
  @Description("Validates that parallel foreach can be used correctly within a nested error handler")
  public void parallelForEachInNestedErrorHandler() throws Exception {
    CoreEvent event = flowRunner("parallelForEachInNestedErrorHandler").run();
    assertThat(event.getMessage().getPayload().getValue(), is("hello"));
  }

  @Test
  @Description("By default routes are run concurrently and multiple threads are used.")
  public void concurrent() throws Exception {
    flowRunner("concurrent").withPayload(fruitList).withVariable("latch", new Latch()).run();
    assertThat(getCapturedThreads(), hasSize(3));
  }

  @Test
  @Issue("MULE-18227")
  @Description("Check that parallel execution routes do not cause race conditions when handling SdkInternalContext")
  public void parallelForEachWithSdkOperation() throws Exception {
    flowRunner("parallelForEachWithSdkOperation").run();
  }

  @Test
  @Issue("MULE-20067")
  public void pagedResults() throws Exception {
    flowRunner("pagedResults").run();
  }

  public static final class ThrowNpeProcessor extends AbstractComponent implements Processor {

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      throw new NullPointerException("nonMule");
    }
  }
}
