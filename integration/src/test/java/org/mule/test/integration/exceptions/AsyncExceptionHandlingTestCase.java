/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.integration.exceptions;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.Assert.assertNotNull;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tests.api.TestQueueManager;

import javax.inject.Inject;

import org.junit.Rule;
import org.junit.Test;

public class AsyncExceptionHandlingTestCase extends AbstractIntegrationTestCase {

  @Inject
  private TestQueueManager queueManager;

  @Rule
  public DynamicPort dynamicPort1 = new DynamicPort("port1");
  @Rule
  public DynamicPort dynamicPort2 = new DynamicPort("port2");

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/exceptions/async-exception-handling-flow.xml";
  }

  @Test
  public void testAsyncExceptionHandlingTestCase() throws Exception {
    flowRunner("SearchWebServiceBridge").runExpectingException();
    assertNotNull(queueManager.read("back-channel", RECEIVE_TIMEOUT, MILLISECONDS));
  }
}
