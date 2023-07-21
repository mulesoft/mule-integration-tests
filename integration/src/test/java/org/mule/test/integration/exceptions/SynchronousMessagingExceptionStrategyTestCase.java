/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.integration.exceptions;

import org.junit.Test;

public class SynchronousMessagingExceptionStrategyTestCase extends AbstractExceptionStrategyTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/exceptions/synch-messaging-exception-strategy.xml";
  }

  @Test
  public void testComponent() throws Exception {
    flowRunner("Component").withPayload(TEST_PAYLOAD).dispatch();
    exceptionListener.waitUntilAllNotificationsAreReceived();
    systemExceptionListener.assertNotInvoked();
  }

  @Test
  public void testProcessorInboundRouter() throws Exception {
    flowRunner("ProcessorInboundRouter").withPayload(TEST_PAYLOAD).dispatch();
    exceptionListener.waitUntilAllNotificationsAreReceived();
    systemExceptionListener.assertNotInvoked();
  }

  @Test
  public void testProcessorOutboundRouter() throws Exception {
    flowRunner("ProcessorOutboundRouter").withPayload(TEST_PAYLOAD).dispatch();
    exceptionListener.waitUntilAllNotificationsAreReceived();
    systemExceptionListener.assertNotInvoked();
  }

}
