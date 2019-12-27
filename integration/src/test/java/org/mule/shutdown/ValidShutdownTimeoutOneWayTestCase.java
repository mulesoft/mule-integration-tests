/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.shutdown;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.mule.functional.api.component.TestConnectorQueueHandler;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.message.Message;
import org.mule.tck.junit4.rule.SystemProperty;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.junit.Rule;
import org.junit.Test;

public class ValidShutdownTimeoutOneWayTestCase extends AbstractShutdownTimeoutRequestResponseTestCase {

  @Rule
  public SystemProperty contextShutdownTimeout = new SystemProperty("contextShutdownTimeout", "5000");

  @Override
  protected String getConfigFile() {
    return "shutdown-timeout-one-way-config.xml";
  }

  @Override
  protected boolean isGracefulShutdown() {
    return true;
  }

  @Test
  public void testStaticComponent() throws Throwable {
    doShutDownTest("staticComponentResponse", "staticComponentFlow");
  }

  @Test
  public void testScriptComponent() throws Throwable {
    doShutDownTest("scriptComponentResponse", "scriptComponentFlow");
  }

  @Test
  public void testSetPayload() throws Throwable {
    doShutDownTest("setPayloadResponse", "setPayloadFlow");
  }

  @Test
  public void testSetPayloadChoice() throws Throwable {
    doShutDownTest("setPayloadResponse", "setPayloadChoiceFlow");
  }

  private void doShutDownTest(final String payload, final String flowName) throws Throwable {
    final TestConnectorQueueHandler queueHandler = new TestConnectorQueueHandler(registry);

    final Future<?> requestTask = executor.submit(() -> {
      try {
        flowRunner(flowName).withPayload(payload).dispatch();

        Message response = queueHandler.read("response", RECEIVE_TIMEOUT).getMessage();
        assertThat("Was not able to process message ", getPayloadAsString(response), is(payload));
      } catch (Exception e) {
        throw new MuleRuntimeException(e);
      }
    });

    // Make sure to give the request enough time to get to the waiting portion of the feed.
    waitLatch.await();
    contextStopLatch.release();

    muleContext.stop();

    try {
      requestTask.get();
    } catch (ExecutionException e) {
      throw e.getCause();
    }
  }
}
