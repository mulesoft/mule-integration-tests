/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.tck.MuleTestUtils.getExceptionListeners;
import static org.mule.tck.MuleTestUtils.getMessageProcessors;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.AbstractMessageProcessorOwner;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.test.AbstractIntegrationTestCase;

import javax.inject.Inject;
import javax.inject.Named;

import org.junit.Test;

public class ErrorHandlerLifecycleTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/exceptions/default-error-handler-lifecycle.xml";
  }

  @Inject
  @Named("flowA")
  private FlowConstruct flowA;

  @Inject
  @Named("flowB")
  private FlowConstruct flowB;

  @Inject
  @Named("flowC")
  private FlowConstruct flowC;

  @Inject
  @Named("flowD")
  private FlowConstruct flowD;

  @Test
  public void testLifecycleErrorHandlerInfLow() throws Exception {
    LifecycleCheckerMessageProcessor lifecycleCheckerMessageProcessorFlowA =
        (LifecycleCheckerMessageProcessor) locator.find(Location.builder().globalName(flowA.getName()).addErrorHandlerPart()
            .addIndexPart(0).addProcessorsPart().addIndexPart(0).build()).get();
    LifecycleCheckerMessageProcessor lifecycleCheckerMessageProcessorFlowB =
        (LifecycleCheckerMessageProcessor) locator.find(Location.builder().globalName(flowB.getName()).addErrorHandlerPart()
            .addIndexPart(0).addProcessorsPart().addIndexPart(0).build()).get();

    assertThat(lifecycleCheckerMessageProcessorFlowA.isInitialized(), is(true));
    assertThat(lifecycleCheckerMessageProcessorFlowB.isInitialized(), is(true));
    ((Lifecycle) flowA).stop();
    assertThat(lifecycleCheckerMessageProcessorFlowA.isStopped(), is(true));
    assertThat(lifecycleCheckerMessageProcessorFlowB.isStopped(), is(false));
  }

  @Test
  public void testLifecycleDefaultErrorHandler() throws Exception {
    AbstractMessageProcessorOwner flowCExceptionStrategy =
        (AbstractMessageProcessorOwner) getExceptionListeners(flowC).get(1);
    AbstractMessageProcessorOwner flowDExceptionStrategy =
        (AbstractMessageProcessorOwner) getExceptionListeners(flowD).get(1);
    LifecycleCheckerMessageProcessor lifecycleCheckerMessageProcessorFlowC =
        (LifecycleCheckerMessageProcessor) getMessageProcessors(flowCExceptionStrategy).get(0);
    LifecycleCheckerMessageProcessor lifecycleCheckerMessageProcessorFlowD =
        (LifecycleCheckerMessageProcessor) getMessageProcessors(flowDExceptionStrategy).get(0);

    assertThat(lifecycleCheckerMessageProcessorFlowC.isInitialized(), is(true));
    assertThat(lifecycleCheckerMessageProcessorFlowD.isInitialized(), is(true));
    ((Lifecycle) flowC).stop();
    assertThat(lifecycleCheckerMessageProcessorFlowC.isStopped(), is(false));
    assertThat(lifecycleCheckerMessageProcessorFlowD.isStopped(), is(false));
    ((Lifecycle) flowD).stop();
    assertThat(lifecycleCheckerMessageProcessorFlowC.isStopped(), is(true));
    assertThat(lifecycleCheckerMessageProcessorFlowD.isStopped(), is(true));
  }

  public static class LifecycleCheckerMessageProcessor extends AbstractComponent implements Processor, Lifecycle {

    private boolean initialized;
    private boolean disposed;
    private boolean started;
    private boolean stopped;

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      return event;
    }

    @Override
    public void dispose() {
      disposed = true;
    }

    @Override
    public void initialise() throws InitialisationException {
      initialized = true;
    }

    @Override
    public void start() throws MuleException {
      started = true;
    }

    @Override
    public void stop() throws MuleException {
      stopped = true;
    }

    public boolean isInitialized() {
      return initialized;
    }

    public boolean isDisposed() {
      return disposed;
    }

    public boolean isStarted() {
      return started;
    }

    public boolean isStopped() {
      return stopped;
    }
  }
}
