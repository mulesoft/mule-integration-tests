/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.components;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import org.mule.runtime.api.component.ExecutableComponent;
import org.mule.runtime.api.event.Event;
import org.mule.runtime.api.event.InputEvent;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.test.AbstractIntegrationTestCase;

import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;

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

  @Override
  protected String getConfigFile() {
    return "org/mule/test/components/flow-execution-config.xml";
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

  private void executeTest(ExecutableComponent executableComponent, Optional<String> errorIdentifierExpected)
      throws InterruptedException, java.util.concurrent.ExecutionException {
    Event resultEvent = executableComponent.execute(createInputEvent())
        .get();
    assertThat(resultEvent.getError().isPresent(), is(errorIdentifierExpected.isPresent()));
    assertThat(resultEvent.getMessage().getPayload().getValue(), is(3));
    errorIdentifierExpected.ifPresent((errorIdentifier) -> {
      assertThat(resultEvent.getError().get().getErrorType().getIdentifier(), is(errorIdentifier));
    });
  }

  private InputEvent createInputEvent() {
    return InputEvent.create()
        .addVariable("myVar", 2)
        .message(Message.builder().payload(TypedValue.of(1))
            .build());
  }

  @Override
  protected boolean doTestClassInjection() {
    return true;
  }
}
