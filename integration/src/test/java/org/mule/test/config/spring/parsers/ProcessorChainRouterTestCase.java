/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config.spring.parsers;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import org.mule.runtime.api.component.ExecutableComponent;
import org.mule.runtime.api.event.Event;
import org.mule.runtime.api.event.InputEvent;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.util.Reference;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.test.config.dsl.ParsersPluginTest;

import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;
import javax.inject.Named;

import org.hamcrest.core.Is;
import org.junit.Test;

public class ProcessorChainRouterTestCase extends AbstractIntegrationTestCase implements ParsersPluginTest {

  @Inject
  @Named("compositeChainRouter")
  private ExecutableComponent chainRouter;

  @Inject
  @Named("compositeChainRouterError")
  private ExecutableComponent chainRouterError;

  @Inject
  @Named("byPassFlow")
  private ExecutableComponent byPassFlow;


  @Override
  protected String getConfigFile() {
    return "org/mule/config/spring/parsers/processor-chain-router-config.xml";
  }

  @Test
  public void executeUsingInputEvent() throws Exception {
    InputEvent event = createInputEvent();

    CompletableFuture<Event> completableFuture = chainRouter.execute(event);
    Event returnedEvent = completableFuture.get();
    assertProcessorChainResult(returnedEvent);
  }

  @Test
  public void executeUsingEvent() throws Exception {
    InputEvent event = createInputEvent();
    Event flowResultEvent = byPassFlow.execute(event).get();

    CompletableFuture<Event> completableFuture = chainRouter.execute(flowResultEvent);
    Event returnedEvent = completableFuture.get();
    assertProcessorChainResult(returnedEvent);
  }

  @Test
  public void executeWithError() throws Exception {
    InputEvent event = createInputEvent();

    CompletableFuture<Event> completableFuture = chainRouterError.execute(event);
    Event returnedEvent = completableFuture.get();
    assertThat(returnedEvent, notNullValue());
    assertThat(returnedEvent.getError().isPresent(), is(true));
    assertThat(returnedEvent.getError().get().getErrorType().getIdentifier(), is("CLIENT_SECURITY"));
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

  @Override
  protected boolean doTestClassInjection() {
    return true;
  }
}
