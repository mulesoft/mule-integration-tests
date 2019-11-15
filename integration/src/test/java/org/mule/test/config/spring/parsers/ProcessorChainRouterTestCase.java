/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config.spring.parsers;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mule.test.allure.AllureConstants.RoutersFeature.ProcessorChainRouterStory.PROCESSOR_CHAIN_ROUTER;
import static org.mule.test.allure.AllureConstants.RoutersFeature.ROUTERS;
import org.mule.functional.api.component.TestConnectorQueueHandler;
import org.mule.runtime.api.component.execution.ComponentExecutionException;
import org.mule.runtime.api.component.execution.ExecutableComponent;
import org.mule.runtime.api.component.execution.ExecutionResult;
import org.mule.runtime.api.component.execution.InputEvent;
import org.mule.runtime.api.event.Event;
import org.mule.runtime.api.message.Message;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.test.IntegrationTestCaseRunnerConfig;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;
import javax.inject.Named;

import org.junit.After;
import org.junit.Test;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

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

  @Override
  protected String getConfigFile() {
    return "org/mule/config/spring/parsers/processor-chain-router-config.xml";
  }

  private ExecutionResult executionResult;

  @After
  public void after() {
    if (executionResult != null) {
      executionResult.complete();
    }
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
    TestConnectorQueueHandler queueHandler = new TestConnectorQueueHandler(registry);
    assertThat(queueHandler.read("asyncQueue", RECEIVE_TIMEOUT) != null, is(true));
    assertThat(queueHandler.read("sgRoute1Queue", RECEIVE_TIMEOUT) != null, is(true));
    assertThat(queueHandler.read("sgRoute2Queue", RECEIVE_TIMEOUT) != null, is(true));
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
