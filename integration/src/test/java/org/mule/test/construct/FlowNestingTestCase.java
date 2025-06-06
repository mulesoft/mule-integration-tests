/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.construct;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.runtime.api.message.Message;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.tests.api.TestQueueManager;

import org.junit.Rule;
import org.junit.Test;

import jakarta.inject.Inject;

public class FlowNestingTestCase extends AbstractIntegrationTestCase {

  @Inject
  private TestQueueManager queueManager;

  @Rule
  public DynamicPort dynamicPort = new DynamicPort("port1");

  @Override
  protected String getConfigFile() {
    return "org/mule/test/construct/flow-nesting-config.xml";
  }

  @Test
  public void testNestingChoiceAccepted() throws Exception {
    flowRunner("NestedChoice").withPayload(new Apple())
        .withVariable("AcquirerCountry", "MyCountry")
        .withVariable("Amount", "4999")
        .run();

    Message result = queueManager.read("outChoice", RECEIVE_TIMEOUT, MILLISECONDS).getMessage();
    assertNotNull(result);
    assertEquals("ABC", getPayloadAsString(result));
  }

  @Test
  public void testNestingChoiceRejected() throws Exception {
    flowRunner("NestedChoice").withPayload(new Apple())
        .withVariable("AcquirerCountry", "MyCountry")
        .withVariable("Amount", "5000")
        .run();

    Message result = queueManager.read("outChoice", RECEIVE_TIMEOUT, MILLISECONDS).getMessage();
    assertNotNull(result);
    assertEquals("AB", getPayloadAsString(result));
  }
}


