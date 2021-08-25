/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.components;

import static java.lang.Runtime.getRuntime;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;
import static org.mule.runtime.api.metadata.MediaType.APPLICATION_JAVA;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.BLOCKING;
import static org.mule.tck.probe.PollingProber.probe;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.test.runner.RunnerDelegateTo;
import org.mule.tests.api.TestQueueManager;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;

import io.qameta.allure.Issue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runners.Parameterized;

@RunnerDelegateTo(Parameterized.class)
public class RedeliveryPolicyTestCase extends AbstractIntegrationTestCase {

  private static CountDownLatch latch;
  private static AtomicInteger awaiting = new AtomicInteger();

  public static class LatchAwaitBlockingProcessor extends AbstractComponent implements Processor {

    @Override
    public ProcessingType getProcessingType() {
      return BLOCKING;
    }

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      awaiting.incrementAndGet();
      try {
        latch.await();
      } catch (InterruptedException e) {
        throw new MuleRuntimeException(e);
      }

      return event;
    }

  }

  @Parameterized.Parameters
  public static List<String> parameters() {
    return asList(DEFAULT_PROCESSING_STRATEGY_CLASSNAME, PROACTOR_PROCESSING_STRATEGY_CLASSNAME);
  }

  @Inject
  private TestQueueManager queueManager;

  private final String processingStrategyFactoryClassname;

  public RedeliveryPolicyTestCase(String processingStrategyFactoryClassname) {
    this.processingStrategyFactoryClassname = processingStrategyFactoryClassname;
  }

  @Override
  protected void doSetUpBeforeMuleContextCreation() throws Exception {
    super.doSetUpBeforeMuleContextCreation();
    setDefaultProcessingStrategyFactory(processingStrategyFactoryClassname);
  }

  @Before
  public void before() {
    latch = new CountDownLatch(1);
    awaiting.set(0);
  }

  @After
  public void after() throws Exception {
    latch.countDown();
  }

  @Override
  protected void doTearDownAfterMuleContextDispose() throws Exception {
    super.doTearDownAfterMuleContextDispose();
    clearDefaultProcessingStrategyFactory();
  }

  @Override
  protected String getConfigFile() {
    return "org/mule/test/components/redelivery-policy-config.xml";
  }

  @Test
  public void hashWorksOverDataWeaveObject() throws Exception {
    sendDataWeaveObjectMessageExpectingError("redeliveryPolicyFlowDispatch");
    sendDataWeaveObjectMessageExpectingError("redeliveryPolicyFlowDispatch");
    assertThat(queueManager.read("redeliveredMessageQueue", RECEIVE_TIMEOUT, MILLISECONDS), notNullValue());
  }

  @Test
  public void redeliveryPolicyDoesntUseCpuLite() throws Exception {
    assumeThat(processingStrategyFactoryClassname, is(PROACTOR_PROCESSING_STRATEGY_CLASSNAME));

    final int dispatchs = (getRuntime().availableProcessors() * 2) + 1;

    for (int i = 0; i < dispatchs; ++i) {
      sendDataWeaveObjectMessage("redeliveryPolicyFlowLongDispatch");
    }
    probe(10000, 100, () -> {
      assertThat(awaiting.get(), is(dispatchs));
      return true;
    });
  }

  @Test
  public void javaPojoPayload() throws Exception {
    final PojoPayload pojoPayload = new PojoPayload();
    flowRunner("redeliveryPolicy3FlowDispatch")
        .withPayload(pojoPayload)
        .withMediaType(APPLICATION_JAVA)
        .run();

    assertThat(queueManager.read("processed", RECEIVE_TIMEOUT, MILLISECONDS), notNullValue());
    assertThat(pojoPayload.isHashCodeCalled(), is(true));
  }

  @Test
  @Issue("MULE-19085")
  public void redeliveryPolicyAndErrorHandler() throws Exception {
    flowRunner("redeliveryPolicyAndErrorHandlerFlowDispatch")
        .runExpectingException();

    assertThat("Error handler was not called",
               queueManager.read("errorHandlerMessageQueue", RECEIVE_TIMEOUT, MILLISECONDS), notNullValue());
    assertThat("Error handler was called more than once",
               queueManager.read("errorHandlerMessageQueue", RECEIVE_TIMEOUT, MILLISECONDS), nullValue());
  }

  private static class PojoPayload {

    private boolean hashCodeCalled = false;

    @Override
    public boolean equals(Object obj) {
      return super.equals(obj);
    }

    @Override
    public int hashCode() {
      hashCodeCalled = true;
      return super.hashCode();
    }

    public boolean isHashCodeCalled() {
      return hashCodeCalled;
    }
  }

  private void sendDataWeaveObjectMessageExpectingError(String flowName) throws Exception {
    flowRunner(flowName)
        .withPayload("{ \"name\" : \"bruce\"}")
        .withMediaType(MediaType.APPLICATION_JSON)
        .runExpectingException();
  }

  private void sendDataWeaveObjectMessage(String flowName) throws Exception {
    flowRunner(flowName)
        .withPayload("{ \"name\" : \"bruce\"}")
        .withMediaType(MediaType.APPLICATION_JSON)
        .run();
  }

}
