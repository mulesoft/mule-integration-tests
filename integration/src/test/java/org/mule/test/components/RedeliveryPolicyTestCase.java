/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.components;

import static java.lang.Runtime.getRuntime;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;
import static org.mule.runtime.api.metadata.MediaType.APPLICATION_JAVA;
import static org.mule.tck.probe.PollingProber.probe;

import org.mule.functional.api.component.EventCallback;
import org.mule.functional.api.component.TestConnectorQueueHandler;
import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.test.runner.RunnerDelegateTo;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runners.Parameterized;

import io.qameta.allure.Issue;

@RunnerDelegateTo(Parameterized.class)
public class RedeliveryPolicyTestCase extends AbstractIntegrationTestCase {

  private static CountDownLatch latch;
  private static AtomicInteger awaiting = new AtomicInteger();

  public static class LatchAwaitCallback extends AbstractComponent implements EventCallback {

    @Override
    public void eventReceived(CoreEvent event, Object component, MuleContext muleContext) throws Exception {
      awaiting.incrementAndGet();
      latch.await();
    }

  }

  @Parameterized.Parameters
  public static List<String> parameters() {
    return asList(DEFAULT_PROCESSING_STRATEGY_CLASSNAME, PROACTOR_PROCESSING_STRATEGY_CLASSNAME);
  }

  private TestConnectorQueueHandler queueHandler;

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
    queueHandler = new TestConnectorQueueHandler(registry);
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
    assertThat(queueHandler.read("redeliveredMessageQueue", RECEIVE_TIMEOUT), notNullValue());
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

    TestConnectorQueueHandler queueHandler = new TestConnectorQueueHandler(registry);
    assertThat(queueHandler.read("processed", RECEIVE_TIMEOUT), notNullValue());

    assertThat(pojoPayload.isHashCodeCalled(), is(true));
  }

  @Test
  @Issue("MULE-19085")
  public void redeliveryPolicyAndErrorHandler() throws Exception {
    flowRunner("redeliveryPolicyAndErrorHandlerFlowDispatch")
        .runExpectingException();

    TestConnectorQueueHandler queueHandler = new TestConnectorQueueHandler(registry);
    assertThat("Error handler was not called",
               queueHandler.read("errorHandlerMessageQueue", RECEIVE_TIMEOUT), notNullValue());
    assertThat("Error handler was called more than once",
               queueHandler.read("errorHandlerMessageQueue", RECEIVE_TIMEOUT), nullValue());
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
