/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.integration.exceptions;

import org.junit.Test;

public class AsynchronousMessagingExceptionStrategyTestCase extends AbstractExceptionStrategyTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/exceptions/asynch-messaging-exception-strategy.xml";
  }

  @Test
  public void testScriptComponentException() throws Exception {
    flowRunner("ScriptComponentException").withPayload(TEST_PAYLOAD).dispatch();
    exceptionListener.waitUntilAllNotificationsAreReceived();
    systemExceptionListener.assertNotInvoked();
  }

  @Test
  public void testCustomProcessorException() throws Exception {
    flowRunner("CustomProcessorException").withPayload(TEST_PAYLOAD).dispatch();
    exceptionListener.waitUntilAllNotificationsAreReceived();
    systemExceptionListener.assertNotInvoked();
  }

}


