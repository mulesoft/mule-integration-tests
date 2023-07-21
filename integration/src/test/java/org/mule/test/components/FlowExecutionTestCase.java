/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.components;

import static java.util.Optional.empty;
import static java.util.Optional.of;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.mule.runtime.api.component.execution.ComponentExecutionException;
import org.mule.runtime.api.component.execution.ExecutableComponent;
import org.mule.runtime.api.component.execution.ExecutionResult;
import org.mule.runtime.api.component.execution.InputEvent;
import org.mule.runtime.api.event.Event;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.TypedValue;

import org.mule.test.AbstractIntegrationTestCase;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;
import javax.inject.Named;

import org.junit.After;
import org.junit.Test;

public class FlowExecutionTestCase extends AbstractIntegrationTestCase {

  @Inject
  @Named("flow")
  private ExecutableComponent flow;

  @Inject
  @Named("flowWithErrorContinue")
  private ExecutableComponent flowWithErrorContinue;

  @Inject
  @Named("flowWithErrorPropagate")
  private ExecutableComponent flowWithErrorPropagate;

  @Inject
  @Named("flowWithCustomError")
  private ExecutableComponent flowWithCustomError;

  @Inject
  @Named("flow-with-on-error-propagate-and-on-error-continue-composition")
  private ExecutableComponent flowWithOnErrorPropagateAndOnErrorContinueComposition;

  @Override
  protected String getConfigFile() {
    return "org/mule/test/components/flow-execution-config.xml";
  }

  private ExecutionResult executionResult;

  @After
  public void after() {
    if (executionResult != null) {
      executionResult.complete();
    }
  }

  @Test
  public void executeFlow() throws Exception {
    executeTest(flow, empty());
  }

  @Test
  public void executeFlowWithErrorContinue() throws Exception {
    executeTest(flowWithErrorContinue, empty());
  }

  @Test
  public void executeFlowWithErrorPropagate() throws Exception {
    executeTest(flowWithErrorPropagate, of("EXPRESSION"));
  }

  @Test
  public void executeFlowWithErrorPropagateWithCustomError() throws Exception {
    executeTest(flowWithCustomError, of("CUSTOM_ERROR_TYPE"));
  }

  @Test
  public void executeFlowWithOnErrorPropagateAndOnErrorContinueComposition() throws Exception {
    executeTest(flowWithOnErrorPropagateAndOnErrorContinueComposition, of("ERROR"));
  }

  private void executeTest(ExecutableComponent executableComponent, Optional<String> errorIdentifierExpected)
      throws InterruptedException {
    Event resultEvent;
    try {
      executionResult = executableComponent.execute(createInputEvent()).get();
      resultEvent = executionResult.getEvent();
      errorIdentifierExpected.ifPresent(error -> fail());
    } catch (ExecutionException e) {
      resultEvent = ((ComponentExecutionException) e.getCause()).getEvent();
    }
    assertThat(resultEvent.getError().isPresent(), is(errorIdentifierExpected.isPresent()));
    assertThat(resultEvent.getMessage().getPayload().getValue(), is(3));
    Event finalResultEvent = resultEvent;
    errorIdentifierExpected.ifPresent((errorIdentifier) -> {
      assertThat(finalResultEvent.getError().get().getErrorType().getIdentifier(), is(errorIdentifier));
    });
  }

  private InputEvent createInputEvent() {
    return InputEvent.create()
        .addVariable("myVar", 2)
        .message(Message.builder().payload(TypedValue.of(1))
            .build());
  }
}
