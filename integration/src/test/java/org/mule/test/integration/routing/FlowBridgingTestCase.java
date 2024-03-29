/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.routing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.mule.runtime.api.message.Message;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Test;

public class FlowBridgingTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/routing/bridge-mule-flow.xml";
  }

  @Test
  public void testSynchronousBridging() throws Exception {
    Message result = flowRunner("bridge").withPayload(TEST_PAYLOAD).run().getMessage();
    assertNotNull(result);
    assertEquals("Received: test", getPayloadAsString(result));
  }
}
