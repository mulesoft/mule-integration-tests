/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.shutdown;

import static org.junit.Assert.assertTrue;

import org.mule.functional.api.component.TestConnectorQueueHandler;
import org.mule.runtime.api.exception.MuleException;
import org.mule.tck.junit4.rule.SystemProperty;

import org.junit.Rule;
import org.junit.Test;

public class ExpiredShutdownTimeoutOneWayTestCase extends AbstractShutdownTimeoutRequestResponseTestCase {

  @Rule
  public SystemProperty contextShutdownTimeout = new SystemProperty("contextShutdownTimeout", "100");

  @Override
  protected String getConfigFile() {
    return "shutdown-timeout-one-way-config.xml";
  }

  @Test
  public void testStaticComponent() throws Exception {
    doShutDownTest("staticComponentFlow");
  }

  @Test
  public void testScriptComponent() throws Exception {
    doShutDownTest("scriptComponentFlow");
  }

  @Test
  public void testExpressionTransformer() throws Exception {
    doShutDownTest("expressionTransformerFlow");
  }

  private void doShutDownTest(final String flowName) throws MuleException, InterruptedException {
    final TestConnectorQueueHandler queueHandler = new TestConnectorQueueHandler(registry);
    final boolean[] results = new boolean[] {false};

    Thread t = new Thread(() -> {
      try {
        flowRunner(flowName).withPayload(TEST_MESSAGE).dispatch();
        results[0] = queueHandler.read("response", RECEIVE_TIMEOUT) == null;
      } catch (Exception e) {
        e.printStackTrace();
        // Ignore
      }
    });
    t.start();

    // Make sure to give the request enough time to get to the waiting portion of the feed.
    waitLatch.await();

    muleContext.stop();

    t.join();

    assertTrue("Was able to process message ", results[0]);
  }
}
