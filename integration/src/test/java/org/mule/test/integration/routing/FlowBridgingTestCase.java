/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
