/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.construct;

import static org.junit.Assert.assertEquals;
import org.mule.functional.api.component.TestConnectorQueueHandler;
import org.mule.runtime.api.message.Message;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Test;

public class FlowOutboundInMiddleOfFlowTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/construct/flow-outbound-in-middle-of-flow.xml";
  }

  @Test
  public void testOutboundInMiddleOfFlow() throws Exception {
    TestConnectorQueueHandler queueHandler = new TestConnectorQueueHandler(registry);

    flowRunner("flowTest").withPayload("message").run();

    Message msg = queueHandler.read("test.out.1", 1000).getMessage();
    assertEquals("messagehello", getPayloadAsString(msg));

    Message msg2 = queueHandler.read("test.out.2", RECEIVE_TIMEOUT).getMessage();
    assertEquals("messagebye", getPayloadAsString(msg2));

    Message msg3 = queueHandler.read("test.out.3", RECEIVE_TIMEOUT).getMessage();
    assertEquals("egassem", getPayloadAsString(msg3));
  }
}


