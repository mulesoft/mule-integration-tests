/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.messaging.meps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.mule.functional.api.flow.FlowRunner;
import org.mule.runtime.api.message.Message;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Test;

public class InOptionalOutOutOnlyAsyncRouterTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/messaging/meps/pattern_In-Optional-Out_Out-Only-Async-Router-flow.xml";
  }

  @Test
  public void testExchange() throws Exception {
    FlowRunner baseRunner = flowRunner("In-Out_Out-Only-Async-Service").withPayload("some data");

    assertNull(baseRunner.run().getMessage().getPayload().getValue());

    baseRunner.reset();
    Message result = baseRunner.withVariable("foo", "bar").run().getMessage();

    assertNotNull(result);
    assertEquals("got it!", getPayloadAsString(result));
  }
}
