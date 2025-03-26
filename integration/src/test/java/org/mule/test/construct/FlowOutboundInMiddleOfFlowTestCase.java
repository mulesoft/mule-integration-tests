/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.construct;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import static org.junit.Assert.assertEquals;

import org.mule.runtime.api.message.Message;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.tests.api.TestQueueManager;

import org.junit.Test;

import jakarta.inject.Inject;

public class FlowOutboundInMiddleOfFlowTestCase extends AbstractIntegrationTestCase {

  @Inject
  private TestQueueManager queueManager;

  @Override
  protected String getConfigFile() {
    return "org/mule/test/construct/flow-outbound-in-middle-of-flow.xml";
  }

  @Test
  public void testOutboundInMiddleOfFlow() throws Exception {
    flowRunner("flowTest").withPayload("message").run();

    Message msg = queueManager.read("test.out.1", 1000, MILLISECONDS).getMessage();
    assertEquals("messagehello", getPayloadAsString(msg));

    Message msg2 = queueManager.read("test.out.2", RECEIVE_TIMEOUT, MILLISECONDS).getMessage();
    assertEquals("messagebye", getPayloadAsString(msg2));

    Message msg3 = queueManager.read("test.out.3", RECEIVE_TIMEOUT, MILLISECONDS).getMessage();
    assertEquals("egassem", getPayloadAsString(msg3));
  }
}


