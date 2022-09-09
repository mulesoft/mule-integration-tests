/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.construct;

import static org.mule.functional.junit4.matchers.ThrowableCauseMatcher.hasCause;
import static org.mule.runtime.api.util.MuleSystemProperties.ENABLE_PROPAGATION_OF_EXCEPTIONS_IN_TRACING;
import static org.mule.test.allure.AllureConstants.RoutersFeature.ROUTERS;
import static org.mule.test.allure.AllureConstants.RoutersFeature.ProcessorChainRouterStory.PROCESSOR_CHAIN_ROUTER;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.rules.ExpectedException.none;

import static java.lang.Boolean.parseBoolean;
import static java.lang.System.setProperty;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import org.mule.functional.junit4.rules.HttpServerRule;
import org.mule.runtime.api.component.execution.ComponentExecutionException;
import org.mule.runtime.api.component.execution.ExecutableComponent;
import org.mule.runtime.api.component.execution.ExecutionResult;
import org.mule.runtime.api.component.execution.InputEvent;
import org.mule.runtime.api.event.Event;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.expression.ExpressionRuntimeException;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.test.IntegrationTestCaseRunnerConfig;
import org.mule.tests.api.TestQueueManager;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;
import javax.inject.Named;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

@Feature(ROUTERS)
@Story(PROCESSOR_CHAIN_ROUTER)
public class ProcessorChainRouterTestCase extends AbstractIntegrationTestCase implements IntegrationTestCaseRunnerConfig {

  @Inject
  @Named("compositeChainRouter")
  private ExecutableComponent compositeChainRouter;

  @Inject
  @Named("compositeChainRouterError")
  private ExecutableComponent compositeChainRouterError;

  @Inject
  @Named("chainRouter")
  private ExecutableComponent chainRouter;

  @Inject
  @Named("chainRouterError")
  private ExecutableComponent chainRouterError;

  @Inject
  @Named("chainRouterComponents")
  private ExecutableComponent chainRouterComponents;

  @Inject
  @Named("byPassFlow")
  private ExecutableComponent byPassFlow;

  @Inject
  @Named("flowRefCompositeChainRouter")
  private ExecutableComponent flowRefCompositeChainRouter;

  @Inject
  @Named("nonBlockingCompositeChainRouter")
  private ExecutableComponent nonBlockingCompositeChainRouter;

  @Inject
  @Named("invalidExpressionParamCompositeChainRouter")
  private ExecutableComponent invalidExpressionParamCompositeChainRouter;

  @Inject
  private TestQueueManager queueManager;

  @Rule
  public HttpServerRule httpServerRule = new HttpServerRule("httpPort");

  @Rule
  public ExpectedException expected = none();

  private boolean previousPropagationEnabledInTracing;

  @Override
  protected String getConfigFile() {
    return "org/mule/construct/processor-chain-router-config.xml";
  }

  private ExecutionResult executionResult;

  @Before
  public void before() {
    // This is done because the tests invokes chains directly using a testing component (injected in the test) and
    // there is no simple way to test this and create a correct mule event with the corresponding span parent without changing API.
    // It does not make sense to make the verification of the current span more flexible for this programmatic invocation of an executable
    // component.
    // TODO: Verify if ignoring tracing condition verification can be done in ProcessorChainRouterTestCase (W-11731027)
    previousPropagationEnabledInTracing = parseBoolean(ENABLE_PROPAGATION_OF_EXCEPTIONS_IN_TRACING);
    setProperty(ENABLE_PROPAGATION_OF_EXCEPTIONS_IN_TRACING, "false");
  }

  @After
  public void after() {
    if (executionResult != null) {
      executionResult.complete();
    }

    setProperty(ENABLE_PROPAGATION_OF_EXCEPTIONS_IN_TRACING, Boolean.toString(previousPropagationEnabledInTracing));
  }


  @Test
  public void executeCompositeRouterUsingInputEvent() throws Exception {
    InputEvent event = createInputEvent();

    CompletableFuture<ExecutionResult> completableFuture = compositeChainRouter.execute(event);
    executionResult = completableFuture.get();
    Event returnedEvent = executionResult.getEvent();
    assertProcessorChainResult(returnedEvent);
  }

  @Test
  public void executeCompositeRouterUsingEvent() throws Exception {
    executionResult = byPassFlow.execute(createInputEvent()).get();
    Event flowResultEvent = executionResult.getEvent();

    CompletableFuture<Event> completableFuture = compositeChainRouter.execute(flowResultEvent);
    Event returnedEvent = completableFuture.get();
    assertProcessorChainResult(returnedEvent);
  }

  @Test
  @Description("Ensure that when composite processor chain is used with more complex/async components such as nested flow-ref there are no dead-locks.")
  public void nestedFlowRefUsingInputEvent() throws Exception {
    InputEvent event = createInputEvent();

    CompletableFuture<ExecutionResult> completableFuture = flowRefCompositeChainRouter.execute(event);

    executionResult = completableFuture.get();
    Event returnedEvent = executionResult.getEvent();
    assertProcessorChainResult(returnedEvent);
  }

  @Test
  @Description("Ensure that when composite processor chain is used with more complex/async components such as nested flow-ref there are no dead-locks.")
  public void nestedFlowRefUsingEvent() throws Exception {
    executionResult = byPassFlow.execute(createInputEvent()).get();
    Event flowResultEvent = executionResult.getEvent();

    CompletableFuture<Event> completableFuture = flowRefCompositeChainRouter.execute(flowResultEvent);

    Event returnedEvent = completableFuture.get();
    assertProcessorChainResult(returnedEvent);
  }

  @Test
  public void executeCompositeRouterWithError() throws Exception {
    InputEvent event = createInputEvent();

    CompletableFuture<ExecutionResult> completableFuture = compositeChainRouterError.execute(event);
    try {
      executionResult = completableFuture.get();
      executionResult.getEvent();
      fail();
    } catch (ExecutionException e) {
      ComponentExecutionException componentExecutionException = (ComponentExecutionException) e.getCause();
      Event returnedEvent = componentExecutionException.getEvent();
      assertThat(returnedEvent, notNullValue());
      assertThat(returnedEvent.getError().isPresent(), is(true));
      assertThat(returnedEvent.getError().get().getErrorType().getIdentifier(), is("CLIENT_SECURITY"));
    }
  }

  @Test
  public void executeChainUsingInputEvent() throws Exception {
    InputEvent event = createInputEvent();

    CompletableFuture<ExecutionResult> completableFuture = chainRouter.execute(event);
    executionResult = completableFuture.get();
    Event returnedEvent = executionResult.getEvent();
    assertThat(returnedEvent, notNullValue());
    assertThat(returnedEvent.getMessage().getPayload().getValue(), is("testPayload custom"));
  }

  @Test
  public void executeChainWithError() throws Exception {
    InputEvent event = createInputEvent();

    CompletableFuture<ExecutionResult> completableFuture = chainRouterError.execute(event);
    Event returnedEvent;
    try {
      completableFuture.get();
      fail();
    } catch (ExecutionException e) {
      ComponentExecutionException componentExecutionException = (ComponentExecutionException) e.getCause();
      returnedEvent = componentExecutionException.getEvent();
      assertThat(returnedEvent, notNullValue());
      assertThat(returnedEvent.getMessage().getPayload().getValue(), is("testPayload custom"));
    }
  }

  @Test
  public void executeChainFlowConstructDependantComponents() throws Exception {
    InputEvent event = createInputEvent();
    CompletableFuture<ExecutionResult> completableFuture = chainRouterComponents.execute(event);
    executionResult = completableFuture.get();
    Event returnedEvent = executionResult.getEvent();
    assertThat(returnedEvent, notNullValue());
    assertThat(queueManager.read("asyncQueue", RECEIVE_TIMEOUT, MILLISECONDS), notNullValue());
    assertThat(queueManager.read("sgRoute1Queue", RECEIVE_TIMEOUT, MILLISECONDS), notNullValue());
    assertThat(queueManager.read("sgRoute2Queue", RECEIVE_TIMEOUT, MILLISECONDS), notNullValue());
  }

  @Test
  @Issue("MULE-18161")
  @Description("Ensure that app graceful shutdown timeout is not imposed as an operation timeout on MUnit chains.")
  public void nonBlockingCompositeChainRouter() throws Exception {
    httpServerRule.getSimpleHttpServer().setResponseDelay(RECEIVE_TIMEOUT + 1000);

    InputEvent event = createInputEvent();

    CompletableFuture<ExecutionResult> completableFuture = nonBlockingCompositeChainRouter.execute(event);

    executionResult = completableFuture.get();
    assertThat(executionResult.getEvent(), not(nullValue()));
  }

  @Test
  @Issue("MULE-18200")
  public void invalidExpressionParamCompositeChainRouter() throws Exception {
    InputEvent event = createInputEvent();

    CompletableFuture<ExecutionResult> completableFuture = invalidExpressionParamCompositeChainRouter.execute(event);

    expected.expect(ExecutionException.class);
    expected.expectCause(instanceOf(ComponentExecutionException.class));
    expected.expectCause(hasCause(instanceOf(ExpressionRuntimeException.class)));
    executionResult = completableFuture.get();
  }

  private void assertProcessorChainResult(Event returnedEvent) {
    assertThat(returnedEvent.getMessage().getPayload().getValue(), is("testPayload custom"));
    assertThat(returnedEvent.getVariables().get("myVar").getValue(), is("myVarValue"));
    assertThat(returnedEvent.getVariables().get("mySecondVar").getValue(), is("mySecondVarValue"));
    assertThat(returnedEvent.getVariables().get("myThirdVar").getValue(), is("myThirdVarValue"));
  }

  private InputEvent createInputEvent() {
    return InputEvent.create().message(Message.builder().value("testPayload").build())
        .addVariable("customVar", "Value");
  }

}
