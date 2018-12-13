/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.components;

import static java.lang.Runtime.getRuntime;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mule.tck.probe.PollingProber.probe;

import org.mule.functional.api.component.EventCallback;
import org.mule.functional.api.component.TestConnectorQueueHandler;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class RedeliveryPolicyTestCase extends AbstractIntegrationTestCase {

  private TestConnectorQueueHandler queueHandler;

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
    final int dispatchs = (getRuntime().availableProcessors() * 2) + 1;

    try {
      for (int i = 0; i < dispatchs; ++i) {
        sendDataWeaveObjectMessage("redeliveryPolicyFlowLongDispatch");
      }
      probe(10000, 100, () -> {
        assertThat(awaiting.get(), is(dispatchs));
        return true;
      });
    } finally {
      latch.countDown();
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

  private static CountDownLatch latch;
  private static AtomicInteger awaiting = new AtomicInteger();

  public static class LatchAwaitCallback implements EventCallback {

    @Override
    public void eventReceived(CoreEvent event, Object component, MuleContext muleContext) throws Exception {
      awaiting.incrementAndGet();
      latch.await();
    }

  }

}
